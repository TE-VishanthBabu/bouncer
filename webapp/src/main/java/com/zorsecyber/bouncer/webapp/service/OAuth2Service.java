package com.zorsecyber.bouncer.webapp.service;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.zorsecyber.bouncer.webapp.constant.ApprovalStatus;
import com.zorsecyber.bouncer.webapp.dao.AccessToken;
import com.zorsecyber.bouncer.webapp.dao.RefreshToken;
import com.zorsecyber.bouncer.webapp.dao.User;
import com.zorsecyber.bouncer.webapp.dao.oAuth2Providers;
import com.zorsecyber.bouncer.webapp.exception.CommonException;
import com.zorsecyber.bouncer.webapp.repository.AccessTokenRepository;
import com.zorsecyber.bouncer.webapp.repository.OAuth2ProvidersRepository;
import com.zorsecyber.bouncer.webapp.repository.RefreshTokenRepository;
import com.zorsecyber.bouncer.webapp.repository.UserRepository;
import com.zorsecyber.bouncer.webapp.response.AccessTokenResponse;
import com.zorsecyber.bouncer.webapp.response.UserInfoResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

	@Value("${base.url}")
	private String baseUrl;

	@Value("${spring.security.oauth2.client.registration.azure.client-id}")
	private String clientId;

	@Value("${spring.security.oauth2.client.registration.azure.redirect-uri}")
	private String reDirectUri;

	@Value("${spring.security.oauth2.client.registration.azure.scope}")
	private String scope;

	@Value("${spring.security.oauth2.client.provider.azure.authorization-uri}")
	private String authorizationUri;

	@Value("${spring.security.oauth2.client.provider.azure.token-uri}")
	private String tokenUri;

	@Value("${spring.security.oauth2.client.provider.azure.user-info-uri}")
	private String userInfoUri;

	@Value("${spring.security.oauth2.client.log-out-uri}")
	private String logOutUri;

	@Value("${spring.security.oauth2.client.log-out-redirect-uri}")
	private String logOutRedirectUri;

	@Value("${spring.security.oauth2.client.registration.azure.authorization-grant-type}")
	private String grantType;

	@Value("${spring.security.oauth2.client.code-challenge}")
	private String codeChallenge;

	@Value("${spring.security.oauth2.client.code-challenge-method}")
	private String codeVerifier;

	@Value("${spring.security.oauth2.client.response-method}")
	private String response;

	@Value("${spring.security.oauth2.client.state}")
	private String state;
	
	private final OrganizationsService organizationService;
	private final LicenseService licenseService;
	private final RolesService roleService;

	private final UserRepository userRepository;
	private final AccessTokenRepository tokenRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final OAuth2ProvidersRepository providersRepository;
	private final RestTemplate restTemplate;

	/**
	 * Get Microsoft login
	 * 
	 * @return redirect view
	 */

	public String getMicrosoftLogin() {
		String redirectUrl = authorizationUri + "?client_id=" + clientId + "&scope=" + scope
				+ "&response_type=code&redirect_uri=" + baseUrl + reDirectUri + "&response_mode=" + response + "&state="
				+ state + "&code_challenge=" + codeChallenge + "&code_challenge_method=" + codeVerifier;
		return redirectUrl;

	}

	/**
	 * To get the access token,refresh token and user details and storing in DB.
	 * Updating existing access token,refresh token-if the user already exists.
	 *
	 * @param code
	 * @return user
	 */
	public User getToken(String code) {
		WebClient webClient = WebClient.builder().baseUrl(tokenUri)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
				.defaultHeader(HttpHeaders.ORIGIN, baseUrl).build();
		AccessTokenResponse accessTokenResponse = webClient.post()
				.body(BodyInserters.fromFormData("client_id", clientId).with("code", code)
						.with("redirect_uri", baseUrl + reDirectUri).with("grant_type", grantType)
						.with("code_verifier", codeVerifier))
				.retrieve().bodyToMono(AccessTokenResponse.class).block();

		String accessToken = null;
		String refreshToken = null;
		long expiresIn = 0;
		if (accessTokenResponse != null) {
			accessToken = accessTokenResponse.getAccess_token();
			refreshToken = accessTokenResponse.getRefresh_token();
			expiresIn = accessTokenResponse.getExpires_in();
		} else {
			throw new CommonException("Token not found", HttpStatus.NOT_FOUND);
		}

		String userEmail = null;
		String userId = null;
		UserInfoResponse infoResponse = null;
		if (accessToken != null) {
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.set("Authorization", "Bearer " + accessToken);
			HttpEntity<String> request = new HttpEntity<>(httpHeaders);
			ResponseEntity<UserInfoResponse> responseEntity = restTemplate.exchange(userInfoUri, HttpMethod.GET,
					request, UserInfoResponse.class);
			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				infoResponse = responseEntity.getBody();
				userEmail = infoResponse.getUserPrincipalName();
				userId = infoResponse.getId();
			}
		}
		// access token expiry
		Calendar accessTokenCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		accessTokenCalendar.setTimeInMillis(System.currentTimeMillis() + (expiresIn * 1000));
		// refresh token expiry
		Calendar refreshTokenCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		if (refreshToken != null) {
			refreshTokenCalendar.add(Calendar.DAY_OF_MONTH, 14);
		}
		log.debug("current time " + Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime().toString());
		log.debug("access token expiry " + accessTokenCalendar.getTime().toString());
		log.debug("refresh token expiry " + refreshTokenCalendar.getTime().toString());
		User user = userRepository.findByEmailAndApprovalStatusEqualsAndAuthUserId(userEmail, ApprovalStatus.Approved,
				userId);
		if (ObjectUtils.isNotEmpty(user)) {
			return this.handleMicrosoftLoginUser(user, accessTokenCalendar, refreshTokenCalendar, accessToken,
					refreshToken);
		} else if (ObjectUtils.isNotEmpty(userRepository.findByEmail(userEmail))) {
			user = userRepository.findByEmail(userEmail);
			return this.handleExistingUserByMicrosoft(user, userEmail, userId, accessToken, refreshToken,
					accessTokenCalendar, refreshTokenCalendar);
		} else {
			return this.handleNewMicrosoftUser(userEmail, userId, accessToken, refreshToken, infoResponse,
					accessTokenCalendar, refreshTokenCalendar);
		}
	}

	/**
	 * Handle Already registered Microsoft user.
	 *
	 * @param user
	 * @param accessTokenCalendar
	 * @param refreshTokenCalendar
	 * @param accessToken
	 * @param refreshToken
	 * @return user
	 */
	public User handleMicrosoftLoginUser(User user, Calendar accessTokenCalendar, Calendar refreshTokenCalendar,
			String accessToken, String refreshToken) {
		AccessToken token = tokenRepository.findByUser(user);
		token.setAccessToken(accessToken);
		token.setExpires(new Timestamp(accessTokenCalendar.getTimeInMillis()));
		tokenRepository.save(token);
		organizationService.saveAndUpdateUserOrganization(user);
		lookupAndAssignLicenseAndAssignRoles(user);
		RefreshToken refreshTokenInfo = refreshTokenRepository.findByAccessToken(token);
		refreshTokenInfo.setRefreshToken(refreshToken);
		refreshTokenInfo.setExpires(new Timestamp(refreshTokenCalendar.getTimeInMillis()));
		refreshTokenRepository.save(refreshTokenInfo);
		return user;
	}

	/**
	 * Handle new Microsoft user.
	 *
	 *
	 * @param userEmail
	 * @param oAuthUserId
	 * @param accessToken
	 * @param refreshToken
	 * @param infoResponse
	 * @param accessTokenCalendar
	 * @param refreshTokenCalendar
	 * @return user
	 */
	public User handleNewMicrosoftUser(String userEmail, String oAuthUserId, String accessToken, String refreshToken,
			UserInfoResponse infoResponse, Calendar accessTokenCalendar, Calendar refreshTokenCalendar) {
		User userInfo = new User();
		userInfo.setEmail(userEmail);
		userInfo.setAuthUserId(oAuthUserId);
		userInfo.setName(infoResponse.getDisplayName());
		userInfo.setFirstName(infoResponse.getGivenName());
		userInfo.setLastName(infoResponse.getSurname());
		userInfo.setApprovalStatus(ApprovalStatus.Approved);
		organizationService.saveAndUpdateUserOrganization(userInfo);
		lookupAndAssignLicenseAndAssignRoles(userInfo);
		userInfo = userRepository.save(userInfo);
		AccessToken token = this.saveAccessToken(userInfo, accessToken, accessTokenCalendar);
		this.saveRefreshToken(token, refreshToken, refreshTokenCalendar);
		return userInfo;
	}


	/**
	 * Handle Already registered user but new to microsoft. Updating existing
	 * details by email.
	 *
	 * @param user
	 * @param userEmail
	 * @param oAuthUserId
	 * @param accessToken
	 * @param refreshToken
	 * @param accessTokenCalendar
	 * @param refreshTokenCalendar
	 * @return user
	 */
	public User handleExistingUserByMicrosoft(User user, String userEmail, String oAuthUserId, String accessToken,
			String refreshToken, Calendar accessTokenCalendar, Calendar refreshTokenCalendar) {
		user.setApprovalStatus(ApprovalStatus.Approved);
		user.setAuthUserId(oAuthUserId);
		organizationService.saveAndUpdateUserOrganization(user);
		lookupAndAssignLicenseAndAssignRoles(user);
		AccessToken token = this.saveAccessToken(user, accessToken, accessTokenCalendar);
		this.saveRefreshToken(token, refreshToken, refreshTokenCalendar);
		return user;
	}
	
	/**
	 * looks up and assign user license, then assigns user roles based on license
	 * 
	 * @param user
	 */
	private void lookupAndAssignLicenseAndAssignRoles(User user) {
		licenseService.lookupAndAssignLicense(user);
		roleService.assignRolesBasedOnLicense(user);
	}

	/**
	 * Save access token details.
	 *
	 * @param user
	 * @param accessToken
	 * @param accessTokenCalendar
	 * @return access token
	 */
	public AccessToken saveAccessToken(User user, String accessToken, Calendar accessTokenCalendar) {
		AccessToken token = new AccessToken();
		token.setAccessToken(accessToken);
		token.setUser(user);
		token.setExpires(new Timestamp(accessTokenCalendar.getTimeInMillis()));
		oAuth2Providers providers;
		if (ObjectUtils.isEmpty(providers = providersRepository.findByName("Azure AD"))) {
			providers = new oAuth2Providers();
			providers.setName("Azure AD");
			providers = providersRepository.save(providers);
		}
		token.setProviders(providers);
		return tokenRepository.save(token);
	}

	/**
	 * Save refresh token details.
	 *
	 * @param accessToken
	 * @param refreshToken
	 * @param refreshTokenCalendar
	 * @return refresh token
	 */
	public RefreshToken saveRefreshToken(AccessToken accessToken, String refreshToken, Calendar refreshTokenCalendar) {
		RefreshToken refresh = new RefreshToken();
		refresh.setRefreshToken(refreshToken);
		refresh.setAccessToken(accessToken);
		refresh.setExpires(new Timestamp(refreshTokenCalendar.getTimeInMillis()));
		return refreshTokenRepository.save(refresh);
	}

	/**
	 * Microsoft logout.
	 *
	 * @return microsoft logout url
	 */
	public String handleLogout() {
		String redirectUrl = logOutUri + "?post_logout_redirect_uri=" + baseUrl + logOutRedirectUri + "&client_id="
				+ clientId;
		return redirectUrl;
	}

}
