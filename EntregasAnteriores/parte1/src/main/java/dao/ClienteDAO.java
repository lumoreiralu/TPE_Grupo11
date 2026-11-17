package dao;

import entity.Cliente;

import java.util.List;


public interface ClienteDAO {
    Cliente findById(int id);
    Cliente findByEmail(String email);
    Cliente findByName(String name);
    List<Cliente> findAll();
    List<Cliente> getListaClientesORDFactura();
    void create(Cliente cliente);
    void update(Cliente cliente);
    void delete(int id);
    void deleteAll();
}