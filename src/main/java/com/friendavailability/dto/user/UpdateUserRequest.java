package com.friendavailability.dto.user;

import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String name;
    private String email;

    @Override
    public String toString() {
        return "UpdateUserRequest{name='" + name + "', email='" + email + "'}";
    }
}