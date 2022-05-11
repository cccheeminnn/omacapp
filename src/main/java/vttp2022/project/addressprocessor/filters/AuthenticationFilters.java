package vttp2022.project.addressprocessor.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Component;

@Component
public class AuthenticationFilters implements Filter{

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException 
    {
        System.out.println("FIRST LINE OF doFilter");

        //boilerplate codes
        //first we cast the ServletReq and Resp to Http, cos we dealing with Http
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        //configuration ends

        //Get back the HTTP session
        HttpSession sess = httpRequest.getSession();
        
        System.out.printf(">>>> url: %s\n".formatted(httpRequest.getRequestURI().toString()));
        System.out.printf(">>>> \t name: %s\n".formatted(sess.getAttribute("username")));

        //this checks that if there is no username(cookie authentication) someone is accessing 
        //the url directly without having logged on
        String username = (String)sess.getAttribute("username");
        if ((null == username) || username.trim().length() <= 0) {
            httpResponse.sendRedirect("/login");
            return;
        }
        
        //ends with doFilter
        //parse it to the next filter, you must do this line if not will have problems
        chain.doFilter(httpRequest, httpResponse);
    }
}