package com.superadmin.services.superadmin;


import java.util.UUID;

import com.superadmin.dto.UserDto;

public interface SuperAdminService {

    UserDto makeAdmin(UUID id);
    UserDto makeUser(UUID id);
}
