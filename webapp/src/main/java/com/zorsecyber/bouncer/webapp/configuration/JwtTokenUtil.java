package com.zorsecyber.bouncer.webapp.configuration;


import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;

@Component
public class JwtTokenUtil implements Serializable {
	private static final long serialVersionUID = -978069096863678538L;
    /**
	 * 
	 */
	private static final String AUTHORITY_CLAIM = "Authority";
    private static final String ID_CLAIM = "Id";
    public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60;
    @Value("${jwt.secret.key}")
    private String secret;

    /**
     * Jwt Token generation.
     *
     * @param id
     * @param email
     * @param authority
     * @return
     */
    public String generateToken(Integer id, String  email, Collection<? extends GrantedAuthority> authority) {
        Date notBefore = Date.from(Instant.now());
        Date expiresAt = Date.from(Instant.now().plusSeconds(JWT_TOKEN_VALIDITY));

        JWTCreator.Builder jwtTokenBuilder = JWT.create().withSubject(email);
        if (id != null) {
            jwtTokenBuilder.withClaim(ID_CLAIM, String.valueOf(id));
        }
        if (authority != null) {
            jwtTokenBuilder.withClaim(AUTHORITY_CLAIM, authority.toString());
        }
        return jwtTokenBuilder
                .withExpiresAt(expiresAt)
                .withNotBefore(notBefore)
                .sign(HMAC512(secret.getBytes()));
    }

    public AuthenticationPrinciple parseToken(String token) {
        DecodedJWT jwt = JWT.require(HMAC512(secret.getBytes())).build().verify(token);
        if (Instant.now().isBefore(jwt.getNotBefore().toInstant())) {
            throw new TokenExpiredException("expired");
        }
        if (Instant.now().isAfter(jwt.getExpiresAt().toInstant())) {
            throw new TokenExpiredException("expired");
        }

        return new AuthenticationPrinciple(jwt.getClaim(ID_CLAIM).asString(), jwt.getSubject(), getRolesAsList(jwt));
    }
    
    private List<String> getRolesAsList(DecodedJWT jwt) {
    	return Arrays.asList(jwt.getClaim(AUTHORITY_CLAIM).asString().replaceAll("[\\[\\]]", "").split(", "));
    }

}

