package dao;

import entity.FacturaProducto;

import java.util.List;

public interface FacturaProductoDAO  {
    FacturaProducto findById(int idFactura, int idProducto);
    List<FacturaProducto> findAll();
    void create(FacturaProducto producto);
    void update(FacturaProducto producto);
    void delete(FacturaProducto producto);
    void deleteAll();
}