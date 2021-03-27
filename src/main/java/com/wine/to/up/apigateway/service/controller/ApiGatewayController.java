package com.wine.to.up.apigateway.service.controller;

import com.netflix.zuul.context.RequestContext;
import com.wine.to.up.apigateway.service.dto.WinePositionWithFavorites;
import com.wine.to.up.apigateway.service.jwt.JwtTokenProvider;
import com.wine.to.up.apigateway.service.service.FavoritePositionService;
import com.wine.to.up.catalog.service.api.dto.WinePositionTrueResponse;
import com.wine.to.up.catalog.service.api.feign.FavoriteWinePositionsClient;
import com.wine.to.up.catalog.service.api.feign.WinePositionClient;
import com.wine.to.up.description.ml.api.feign.WineRecommendationServiceClient;
import com.wine.to.up.user.service.api.dto.ItemDto;
import com.wine.to.up.user.service.api.feign.FavoritesServiceClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/catalog-service")
@Validated
@Slf4j
@Api(value = "ApiGatewayController")
public class ApiGatewayController {


    private final FavoritesServiceClient favoritesServiceClient;

    private final FavoriteWinePositionsClient favoriteWinePositionsClient;

    private final WinePositionClient winePositionClient;

    private final FavoritePositionService favoritePositionService;

    private final WineRecommendationServiceClient wineRecommendationServiceClient;


    @ApiOperation(value = "Get favourites wine positions",
            nickname = "getFavouritesPositions",
            tags = {"favorite-positions-controller",})
    @GetMapping("/favorites")
    public List<WinePositionTrueResponse> getFavourites() {
        log.info("Got request for favorite positions");
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        String accessToken = request.getHeader("Authorization").split(" ")[1];

        String id = JwtTokenProvider.getId(accessToken);
        String role = JwtTokenProvider.getRole(accessToken);

        List<ItemDto> itemDtos = favoritesServiceClient.findUsersFavorites(id, role);

        setHeaders();

        if (itemDtos.isEmpty()) return new ArrayList<>();

        log.info("Favorite positions amount: " + itemDtos.size());
        List<String> ids = itemDtos.stream().map(ItemDto::getId).collect(Collectors.toList());

        Map<String, List<String>> query = new HashMap<>();
        query.put("favouritePosition", ids);

        return favoriteWinePositionsClient.getFavourites(query);
    }

    @ApiOperation(value = "Get favourites wine positions",
            nickname = "getFavouritesPositions",
            tags = {"favorite-positions-controller",})
    @GetMapping("/true/trueSettings")
    public List<WinePositionWithFavorites> getWinePositions(@RequestParam(required = false) String page,
                                                            @RequestParam(required = false) String amount,
                                                            @RequestParam(required = false) List<String> sortByPair,
                                                            @RequestParam(required = false) String filterBy) {
        log.info("Got request for positions with settings");
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        String accessToken = request.getHeader("Authorization").split(" ")[1];

        if (accessToken.equals("123")) {
            List<WinePositionTrueResponse> positions = getWinePositionTrueResponses(page, amount, sortByPair, filterBy);
            setHeaders();
            return favoritePositionService.convertWinePositions(positions, new HashSet<>());
        }

        Set<String> ids = getFavoriteIds(accessToken);

        List<WinePositionTrueResponse> positions = getWinePositionTrueResponses(page, amount, sortByPair, filterBy);

        log.info("Wine positions: " + positions.size());

        setHeaders();

        return favoritePositionService.convertWinePositions(positions, ids);
    }

    @GetMapping("/byId/{id}")
    public WinePositionWithFavorites getWineById(@Valid @PathVariable(name = "id") String winePositionId) {
        log.info("Got request for positions by id");
        //List<String> recomendationIds = wineRecommendationServiceClient.recommend(winePositionId);
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        String accessToken = request.getHeader("Authorization").split(" ")[1];

        WinePositionTrueResponse response = favoriteWinePositionsClient.getPositionById(winePositionId);

        if (accessToken.equals("123")) {
            setHeaders();
            return favoritePositionService.getPosition(response, new HashSet<>());
        }

        Set<String> ids = getFavoriteIds(accessToken);
        return favoritePositionService.getPosition(response, ids);
    }

    private Set<String> getFavoriteIds(String accessToken) {
        String id = JwtTokenProvider.getId(accessToken);
        String role = JwtTokenProvider.getRole(accessToken);

        List<ItemDto> itemDtos = favoritesServiceClient.findUsersFavorites(id, role);
        return itemDtos.stream().map(ItemDto::getId).collect(Collectors.toSet());
    }

    private List<WinePositionTrueResponse> getWinePositionTrueResponses(String page, String amount, List<String> sortByPair, String filterBy) {
        Map<String, List<String>> query = new HashMap<>();
        List<String> amountList  = new ArrayList<>();
        amountList.add(amount);
        List<String> pageList  = new ArrayList<>();
        pageList.add(page);
        List<String> filterByList  = new ArrayList<>();
        filterByList.add(filterBy);
        query.put("sortByPair", sortByPair);
        query.put("amount", amountList);
        query.put("page", pageList);
        query.put("filterBy", filterByList);
        List<WinePositionTrueResponse> positions = winePositionClient.getAllWinePositionsTrue(query);
        return positions;
    }

    private void setHeaders() {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletResponse servletResponse = context.getResponse();
        servletResponse.addHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        servletResponse.addHeader("Pragma", "no-cache");
        servletResponse.addHeader("Expires", "0");
    }

}
