package com.example.demo.role.service;

import com.example.demo.exceptions.BadRequestException;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.res.Response;
import com.example.demo.role.entity.Role;
import com.example.demo.role.repo.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;

    @Override
    public Response<Role> createRole(Role role) {

        if (roleRepository.findByName(role.getName()).isPresent()) {
            throw new BadRequestException("Role already exists");
        }

        Role savedRole = roleRepository.save(role);


        return Response.<Role>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Role created successfully")
                .data(savedRole)
                .build();
    }

    @Override
    public Response<Role> updateRole(Role roleRequest) {

        Role role = roleRepository.findById(roleRequest.getId())
                .orElseThrow(() -> new NotFoundException("Role not found"));

        role.setName(roleRequest.getName());

        Role updatedRole = roleRepository.save(role);

        Role updatedRoleDTO = modelMapper.map(updatedRole, Role.class);

        return Response.<Role>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Role updated successfully")
                .data(updatedRoleDTO)
                .build();
    }

    @Override
    public Response<List<Role>> getAllRoles() {

        List<Role> roles = roleRepository.findAll();

        List<Role> roleDTOs = roles.stream()
                .map(role -> modelMapper.map(role, Role.class))
                .collect(Collectors.toList());

        return Response.<List<Role>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Roles retrieved successfully")
                .data(roleDTOs)
                .build();
    }

    @Override
    public Response<?> deleteRole(Long id) {

        if (!roleRepository.existsById(id)) {
            throw new NotFoundException("Role not found with ID: " + id);
        }

        roleRepository.deleteById(id);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Role Deleted Successfully")
                .build();
    }
}