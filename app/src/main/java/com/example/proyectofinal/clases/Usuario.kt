package com.example.proyectofinal.clases

data class Usuario(
    val Id_Usuario: Int,
    val Nombre: String,
    val Correo: String,
    val Clave: String,
    val Id_Tipo: Int,
    val Permisos: String
)