package org.jpos.rest.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionAPI {

    @GET
    @Path("/{id}")
    public Response getTransaction(@PathParam("id") String id) {
        try {
            String result = "Transaction: " + id;
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck() {
        System.out.println("Health check");
        return Response.ok("OK").build();
    }

}
