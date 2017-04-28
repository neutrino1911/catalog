package ru.test;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.test.entities.Node;
import ru.test.service.CatalogService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

@Path("/")
@Produces("application/json; charset=UTF-8")
public class MainRest {

    @EJB
    private CatalogService catalogService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @PUT @Path(value = "/add")
    @Consumes("application/x-www-form-urlencoded")
    public Response add(MultivaluedMap<String, String> formParams) {
        Map<String, String> params = new HashMap<>(formParams.size());
        for (String key : formParams.keySet()) {
            params.put(escapeHtml4(key), escapeHtml4(formParams.getFirst(key)));
        }
        boolean result = catalogService.add(params);
        return getSuccess(result);
    }

    @PUT @Path(value = "/update")
    @Consumes("application/x-www-form-urlencoded")
    public Response update(MultivaluedMap<String, String> formParams) {
        Map<String, String> params = new HashMap<>(formParams.size());
        for (String key : formParams.keySet()) {
            params.put(escapeHtml4(key), escapeHtml4(formParams.getFirst(key)));
        }
        boolean result = catalogService.update(params);
        return getSuccess(result);
    }

    @GET @Path(value = "/get/{id : \\d+}")
    public Response get(@PathParam("id") long id) {
        if (id < 0) {
            return getError(400);
        }
        Node node = catalogService.get(id);
        if (node == null) {
            return getError(404);
        }
        return getSuccess(node);
    }

    @GET @Path(value = "/gettree/{parentId : \\d+}")
    public Response getTree(@PathParam("parentId") long parentId) {
        if (parentId < 0) {
            return getError(400);
        }
        List<Node> list = catalogService.getTree(parentId);
        if (list.isEmpty()) {
            return getError(404);
        }
        return getSuccess(list);
    }

    @DELETE @Path(value = "/remove/{id : \\d+}")
    public Response removeItem(@PathParam("id") long id) {
        if (id < 0) {
            return getError(400);
        }
        boolean status = catalogService.remove(id);
        if (status) {
            return getSuccess(status);
        } else {
            return getError(404);
        }
    }

    private String getJSON(Object o) {
        String body;
        try {
            body = objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
        return body;
    }

    private Response getError(int code) {
        Map<String, String> map = new HashMap<>();
        map.put("state", "error");
        switch (code) {
            case 400:
                map.put("code", "400 Bad Request");
                break;
            case 404:
                map.put("code", "404 Not Found");
                break;
            case 500:
                map.put("code", "500 Internal Server Error");
                break;
        }
        return Response.status(code).entity(getJSON(map)).build();
    }

    private Response getSuccess(Object o) {
        Map<String, Object> map = new HashMap<>();
        map.put("state", "success");
        map.put("result", o);
        return Response.status(200).entity(getJSON(map)).build();
    }
}
