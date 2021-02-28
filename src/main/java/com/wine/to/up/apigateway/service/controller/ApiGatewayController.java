package com.wine.to.up.apigateway.service.controller;

import com.netflix.zuul.context.RequestContext;
import com.wine.to.up.catalog.service.api.dto.WinePositionTrueResponse;
import com.wine.to.up.catalog.service.api.service.FavoriteWinePositionsService;
import com.wine.to.up.user.service.api.dto.ItemDto;
import com.wine.to.up.user.service.api.feign.FavoritesServiceClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/favorites")
@Validated
@Slf4j
@Api(value = "ApiGatewayController")
public class ApiGatewayController {
    private final FavoritesServiceClient favoritesServiceClient;

    private final FavoriteWinePositionsService favoriteWinePositionsService;


    @ApiOperation(value = "Get favourites wine positions",
            nickname = "getFavouritesPositions",
            tags = {"wine-position-true-controller",})
    @GetMapping("/")
    public List<WinePositionTrueResponse> getFavourites() {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        String accessToken = request.getHeader("Authorization").split(" ")[1];

        List<ItemDto> itemDtos = favoritesServiceClient.findUsersFavorites(accessToken);
        List<String> ids = itemDtos.stream().map(ItemDto::getId).collect(Collectors.toList());
        return favoriteWinePositionsService.getFavourites(ids);
    }


}
