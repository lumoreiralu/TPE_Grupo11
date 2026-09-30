package universidad.repositories;

import universidad.entity.Estudiante;

import java.util.List;

public interface EstudianteRepository {

    Estudiante save(Estudiante estudiante);
    Estudiante findByDni(Integer dni);
    List<Estudiante> findAllOrderedByEdadAsc();
    Estudiante findByLU(int numLU);
    List<Estudiante> findByGenero(String genero);
    List<Estudiante> findByCarreraAndCiudad(Integer idCarrera, String ciudad);
}
