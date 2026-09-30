package dao;

import entity.Factura;

import java.util.List;


public interface FacturaDAO  {
    Factura findById(Integer id);
    List<Factura> findAll();
    void create(Factura factura);
    void update(Factura factura);
    void delete(Factura factura);
    void deleteAll();
}