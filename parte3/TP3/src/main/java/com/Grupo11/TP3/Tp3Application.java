package com.Grupo11.TP3;

import com.Grupo11.TP3.utils.CargaDeDatos;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class Tp3Application {
	@Autowired
	private CargaDeDatos cargaDeDatos;

	public static void main(String[] args) {
		SpringApplication.run(Tp3Application.class, args);
	}

	@PostConstruct
	//Le dice a Spring: "Una vez que hayas arrancado y cargado todos los beans en memoria,
	// ejecutá inmediatamente este método

	public void init() throws IOException {
		cargaDeDatos.cargarDatosDesdeCSV();
	}

}
