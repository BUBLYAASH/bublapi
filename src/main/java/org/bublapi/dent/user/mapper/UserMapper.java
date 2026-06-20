package org.bublapi.dent.user.mapper;

import org.bublapi.dent.user.dto.CreateUserRequestDto;
import org.bublapi.dent.user.dto.UpdateUserRequestDto;
import org.bublapi.dent.user.dto.UserResponseDto;
import org.bublapi.dent.user.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    User toEntity(CreateUserRequestDto request);

    @Mapping(target = "clinicId", source = "clinic.id")
    UserResponseDto toResponse(User user);

    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateUserRequestDto request, @MappingTarget User user);
}
