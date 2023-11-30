package com.zorsecyber.bouncer.webapp.service;
//package com.zorsecyber.hauberk.website.service;
//
//import com.zorsecyber.hauberk.website.constant.ApprovalStatus;
//import com.zorsecyber.hauberk.website.dao.Roles;
//import com.zorsecyber.hauberk.website.dao.User;
//import com.zorsecyber.hauberk.website.exception.UserNotFoundException;
//import com.zorsecyber.hauberk.website.repository.UserRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import java.util.Collection;
//import java.util.stream.Collectors;
//
//@Service
//@Slf4j
//public class CustomUserDetailService implements UserDetailsService {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    /**
//     * Authenticating the user by email and password.
//     *
//     * @param email
//     * @return userDetails
//     * @throws UsernameNotFoundException
//     */
//    @Override
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        User user =userRepository.findByEmailAndApprovalStatusEquals(email,ApprovalStatus.Approved);
//        if (user != null) {
//            log.info("Authenticating user");
//            return new org.springframework.security.core.userdetails.User(user.getEmail(),
//                    user.getPassword(),
//                    mapRolesToAuthorities(user.getRoles()));
//        } else {
//            throw new UserNotFoundException("Invalid username or password.");
//        }
//    }
//
//    /**
//     * Getting the roles.
//     *
//     * @param roles
//     * @return collection of roles
//     */
//    private Collection< ? extends GrantedAuthority> mapRolesToAuthorities(Collection <Roles> roles) {
//        Collection < ? extends GrantedAuthority> mapRoles = roles.stream()
//                .map(role -> new SimpleGrantedAuthority(role.getName()))
//                .collect(Collectors.toList());
//        return mapRoles;
//    }
//}
