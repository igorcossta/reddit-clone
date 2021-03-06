package com.reddit.spring.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.reddit.spring.dto.LoginResponse;
import com.reddit.spring.model.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@AllArgsConstructor
public class Jwt {
    private final static String security = "secret";
    private final static Algorithm algorithm = Algorithm.HMAC256(security.getBytes(UTF_8));

    public static String createToken(User user, String issuer) {
        String token = JWT.create()
                .withIssuer(issuer)
                .withExpiresAt(new Date(System.currentTimeMillis() + 20000 * 60))
                .withSubject(user.getUsername())
                .withClaim("authority", user.getAuthorities()
                        .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .sign(algorithm);
        return token;
    }

    public static String createRefreshToken(String username, String issuer) {
        String token = JWT.create()
                .withIssuer(issuer)
                .withExpiresAt(new Date(System.currentTimeMillis() + 5000 * 120))
                .withSubject(username)
                .sign(algorithm);
        return token;
    }

    public static LoginResponse createTokenResponse(User user) {
        return new LoginResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getUsername()
        );
    }

    private static void setCookies(HttpServletResponse res, String token, String refreshToken) {
        int maxAge = 15;
        // token cookie
        Cookie cookieJWT = new Cookie("token", token);
        cookieJWT.setHttpOnly(true);
        cookieJWT.setMaxAge(maxAge);
        // refresh token cookie
        Cookie refreshJWT = new Cookie("refreshToken", refreshToken);
        refreshJWT.setHttpOnly(true);
        refreshJWT.setMaxAge(maxAge);
        // Set cookies
        res.addCookie(cookieJWT);
        res.addCookie(refreshJWT);
    }

    public static UsernamePasswordAuthenticationToken createCredentialsFromToken(String token) {
        DecodedJWT decodedJWT = verifyToken(token);
        // grab the username from token
        String username = decodedJWT.getSubject();
        // grab an string list with authorities
        String[] authorities = decodedJWT.getClaim("authority").asArray(String.class);
        // map authorities from string list to Set of Simple Granted Authority
        Set<GrantedAuthority> authority = new HashSet<>();
        Arrays.stream(authorities).forEach(i -> authority.add(new SimpleGrantedAuthority(i)));
        return new UsernamePasswordAuthenticationToken(username, null, authority);
    }

    private static DecodedJWT verifyToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        } catch (JWTVerificationException ex) {
            log.error("verify token throws an exception -> {}", ex.getMessage());
            throw new JWTVerificationException(ex.getMessage());
        }
    }

    public static String getJwtFromCookie(Cookie[] cookies) {
        if (cookies == null) return "";
        for (Cookie cookie : cookies) {
            if ("token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return "";
    }
}
