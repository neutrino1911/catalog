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

    @GET @Path(value = "/nodes/count")
    public Response getCount() {
        long count = catalogService.getCount();
        if (count < 0) {
            return getError(500);
        }
        return getSuccess(count);
    }

    @GET @Path(value = "/nodes/{parentId : \\d+}/page/{page : \\d+}/sort/{sort : asc|desc}")
    public Response getNodes(
            @PathParam("parentId") long parentId,
            @PathParam("page") long page,
            @PathParam("sort") String sort) {
        if (parentId < 0 || page < 0) {
            return getError(400);
        }
        List<Node> list = catalogService.getNodes(parentId, page, sort);
        if (list == null) {
            return getError(500);
        }
        return getSuccess(list);
    }

    @GET @Path(value = "/node/find/{query}/page/{page : \\d+}")
    public Response findNodes(
            @PathParam("query") String query,
            @PathParam("page") long page) {
        if (query.isEmpty() || page < 0) {
            return getError(400);
        }
        List<Node> nodes = catalogService.findNodes(query, page);
        if (nodes == null) {
            return getError(404);
        }
        return getSuccess(nodes);
    }

    @GET @Path(value = "/node/{id : \\d+}")
    public Response getNode(@PathParam("id") long id) {
        if (id < 0) {
            return getError(400);
        }
        Node node = catalogService.getNode(id);
        if (node == null) {
            return getError(404);
        }
        return getSuccess(node);
    }

    @PUT @Path(value = "/node")
    @Consumes("application/json;charset=UTF-8")
    public Response addNode(String body) {
        try {
            Node node = new ObjectMapper().readValue(body, Node.class);
            if (node.getParentId() < 0) return getError(400);
            if (catalogService.addNode(node) != null) {
                return getSuccess(node);
            } else {
                return getError(500);
            }
        } catch (IOException e) {
            return getError(400);
        }
    }

    @POST @Path(value = "/node/{id : \\d+}")
    @Consumes("application/json;charset=UTF-8")
    public Response updateNode(@PathParam("id") long id, String body) {
        try {
            Node node = new ObjectMapper().readValue(body, Node.class);
            if (node.getId() < 1) return getError(400);
            if (node.getParentId() < 0) return getError(400);
            if (catalogService.updateNode(node) != null) {
                return getSuccess(node);
            } else {
                return getError(500);
            }
        } catch (IOException e) {
            return getError(400);
        }
    }

    @DELETE @Path(value = "/node/{id : \\d+}")
    public Response removeNode(@PathParam("id") long id) {
        if (id < 0) {
            return getError(400);
        }
        if (catalogService.removeNode(id)) {
            return getSuccess(true);
        } else {
            return getError(404);
        }
    }

    @DELETE @Path(value = "/field/{id : \\d+}")
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

    @POST @Path(value = "/mock/{count : \\d+}")
    public Response mockData(@PathParam("count") long count) {
        catalogService.mock(count);
        return getSuccess(true);
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
