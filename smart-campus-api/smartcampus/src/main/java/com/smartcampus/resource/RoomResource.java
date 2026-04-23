package com.smartcampus.resource;
/**
 *
 * @author winil
 */

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // GET /api/v1/rooms — list all rooms
    @GET
    public Response getAllRooms() {
        List<Room> rooms = new ArrayList<>(store.getRooms().values());
        return Response.ok(rooms).build();
    }

    // POST /api/v1/rooms — create a new room
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Room ID is required."))
                    .build();
        }
        if (store.getRooms().containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("Room with ID '" + room.getId() + "' already exists."))
                    .build();
        }
        store.getRooms().put(room.getId(), room);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Room created successfully.");
        response.put("room", room);
        response.put("_links", linkMap(room.getId()));

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // GET /api/v1/rooms/{roomId} — get a specific room
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }
        return Response.ok(room).build();
    }

    // DELETE /api/v1/rooms/{roomId} — delete a room (blocked if sensors present)
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            // Idempotent: already gone, return 404
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }
        if (!room.getSensorIds().isEmpty()) {
            // Business logic: cannot delete room with active sensors
            throw new RoomNotEmptyException(
                "Room '" + roomId + "' cannot be deleted. It still has " +
                room.getSensorIds().size() + " sensor(s) assigned to it. " +
                "Please remove or reassign all sensors before decommissioning this room."
            );
        }
        store.getRooms().remove(roomId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Room '" + roomId + "' deleted successfully.");
        return Response.ok(response).build();
    }

    // Helper: standard error body
    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", message);
        body.put("status", "error");
        return body;
    }

    // Helper: HATEOAS links for a room
    private Map<String, String> linkMap(String roomId) {
        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v1/rooms/" + roomId);
        links.put("all-rooms", "/api/v1/rooms");
        return links;
    }
}
