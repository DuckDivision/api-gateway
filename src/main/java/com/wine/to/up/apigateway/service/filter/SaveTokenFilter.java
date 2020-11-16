package com.wine.to.up.apigateway.service.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.wine.to.up.apigateway.service.repository.UserTokenRepository;
import com.wine.to.up.commonlib.annotations.InjectEventLogger;
import com.wine.to.up.commonlib.logging.EventLogger;
import com.wine.to.up.user.service.api.dto.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;

@RequiredArgsConstructor
@Component
public class SaveTokenFilter extends ZuulFilter {
    @InjectEventLogger
    private EventLogger eventLogger;

    private final UserTokenRepository userTokenRepository;


    @Override
    public String filterType() {
        return POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        String endpointToFilter = RequestContext.getCurrentContext().getRequest().getRequestURI();
        return (endpointToFilter.contains("/user-service/login") || endpointToFilter.contains("/user-service/refresh"));
    }

    @SneakyThrows
    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();

        try (final InputStream responseDataStream = context.getResponseDataStream()) {

            if(responseDataStream == null) {
                return null;
            }

            String responseData = CharStreams.toString(new InputStreamReader(responseDataStream, "UTF-8"));

            context.setResponseBody(responseData);

            ObjectMapper objectMapper = new ObjectMapper();
            AuthenticationResponse userServiceResponse = objectMapper
                    .readValue(context.getResponseBody(), AuthenticationResponse.class);


            userTokenRepository.addToken(userServiceResponse.getAccessToken());

        }
        catch (Exception e) {
            throw new ZuulException(e, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }

        return null;
    }
}
