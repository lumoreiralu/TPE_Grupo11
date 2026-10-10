package com.Grupo11.TP3.services;


import com.Grupo11.TP3.models.Estudiante;
import com.Grupo11.TP3.repository.EstudianteRepository;

@Service("EstudianteServicio")
public class EstudianteService implements BaseService<Estudiante>{

    @Autowired
    private EstudianteRepository estudianteRepository;

    //Inciso 2.a
    @Override
    @Transactional
    public Estudiante save(Estudiante estudiante) throws Exception{
        try{
            return  estudianteRepository.save(estudiante);
        }
        catch(Exception e){
            throw new Exception(e.getMessage());
        }
    }

    //Inciso 2.d
    @Transactional
    public Estudiante buscarEstudiantePorLU(int lu) throws Exception{
        try{
            return estudianteRepository.getEstudianteByLU(lu);
        }catch(Exception e){
            throw new Exception(e.getMessage());
        }
    }

}
