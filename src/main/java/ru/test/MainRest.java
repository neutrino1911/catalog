package ru.test;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.test.entities.Node;
import ru.test.service.CatalogService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/")
@Produces("application/json; charset=UTF-8")
public class MainRest {

    @EJB
    private CatalogService catalogService;

    private ObjectMapper objectMapper = new ObjectMapper();

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
        if (list == null) {
            return getError(500);
        }
        return getSuccess(list);
    }

    @GET @Path(value = "/find")
    public Response find(@QueryParam("q") String text) {
        if (text == null || text.isEmpty()) {
            return getError(400);
        }
        List<Node> nodes = catalogService.find(text);
        if (nodes == null) {
            return getError(404);
        }
        return getSuccess(nodes);
    }

    @PUT @Path(value = "/add")
    @Consumes("application/json;charset=UTF-8")
    public Response add(String body) {
        try {
            Node node = new ObjectMapper().readValue(body, Node.class);
            if (node.getParentId() < 0) return getError(400);
            if (catalogService.add(node) != null) {
                return getSuccess(node);
            } else {
                return getError(500);
            }
        } catch (IOException e) {
            return getError(400);
        }
    }

    @PUT @Path(value = "/update")
    @Consumes("application/json;charset=UTF-8")
    public Response update(String body) {
        try {
            Node node = new ObjectMapper().readValue(body, Node.class);
            if (node.getId() < 1) return getError(400);
            if (node.getParentId() < 0) return getError(400);
            if (catalogService.update(node) != null) {
                return getSuccess(node);
            } else {
                return getError(500);
            }
        } catch (IOException e) {
            return getError(400);
        }
    }

    @DELETE @Path(value = "/remove/{id : \\d+}")
    public Response removeItem(@PathParam("id") long id) {
        if (id < 0) {
            return getError(400);
        }
        if (catalogService.remove(id)) {
            return getSuccess(true);
        } else {
            return getError(404);
        }
    }

    @DELETE @Path(value = "/removefield/{id : \\d+}")
    public Response removeField(@PathParam("id") long id) {
        if (id < 0) {
            return getError(400);
        }
        boolean status = catalogService.removeField(id);
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
