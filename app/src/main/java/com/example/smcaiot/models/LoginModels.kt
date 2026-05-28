package com.example.smcaiot.models

import com.google.gson.annotations.SerializedName

/** Cuerpo de la petición POST /oauth2/login */
data class LoginRequest(
    val identifier: String,
    val password: String
)

/** Respuesta completa del endpoint de login */
data class LoginResponse(
    val message: String,
    val token: String,
    val user: UserInfo
)

data class UserInfo(
    val id: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    val firstLogin: Boolean,
    val role: UserRole,
    val permissions: List<UserPermission>,
    val entity: UserEntity,
    val provider: String,
    val nivel: Int,
    val avatar: String?,
    val isActive: Boolean,
    val lastLogin: String?,
    val loginAttempts: Int,
    val lockUntil: String?,
    val estadoEliminacion: Int
)

data class UserRole(
    val nombre: String,
    val descripcion: String
)

data class UserPermission(
    val name: String,
    val description: String
)

data class UserEntity(
    val id: String,
    val name: String
)
