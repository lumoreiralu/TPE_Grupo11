package com.Grupo11.TP3.services;


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
    @Trasactional
    public Estudiante buscarEstudiantePorLU(int lu){
        var est = estudianteRepository.getEstudianteByLU(lu);
            try{
                return est;
        }catch(Exception e){
                throw new Exception(e.getMessagge());
            }
    }

}
