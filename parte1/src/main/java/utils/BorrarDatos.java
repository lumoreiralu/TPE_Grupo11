package utils;
import dao.ClienteDAO;
import dao.ProductoDAO;
import dao.FacturaDAO;
import dao.FacturaProductoDAO;

import factory.DAOFactory;

public class BorrarDatos {
    private final ClienteDAO clienteDAO;
    private final ProductoDAO productoDAO;
    private final FacturaDAO facturaDAO;
    private final FacturaProductoDAO facturaProductoDAO;

    public BorrarDatos() {
        DAOFactory f = DAOFactory.getInstance(); // toma db.type o default MYSQL
        this.clienteDAO       = f.createClienteDAO();
        this.productoDAO       = f.createProductoDAO();
        this.facturaDAO         = f.createFacturaDAO();
        this.facturaProductoDAO = f.createFacturaProductoDAO();

    }

    public void run() {

        try {
            this.facturaProductoDAO.deleteAll(); //este primero para poder borrar las tablas padres
            this.facturaDAO.deleteAll();
            this.clienteDAO.deleteAll();
            this.productoDAO.deleteAll();
            System.out.println("Borrado completo");
        } catch (Exception e) {
            throw new RuntimeException("Error durante el borrado masivo.", e);
        }
    }
}
