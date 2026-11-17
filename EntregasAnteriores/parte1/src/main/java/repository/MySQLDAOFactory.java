package repository;


import java.sql.Connection;

import dao.ClienteDAO;
import dao.FacturaProductoDAO;
import dao.FacturaDAO;
import dao.ProductoDAO;

import factory.DAOFactory;

public class MySQLDAOFactory extends DAOFactory { // <— EXTENDS DAOFactory

    @Override
    public ClienteDAO createClienteDAO() {
        return new MySQLClienteDAO(getConnection());
    }

    @Override
    public ProductoDAO createProductoDAO() {
        return new MySQLProductoDAO(getConnection());
    }

    @Override
    public FacturaDAO createFacturaDAO() {
        return new MySQLFacturaDAO(getConnection());
    }

    @Override
    public FacturaProductoDAO createFacturaProductoDAO() {
        return new MySQLFacturaProductoDAO(getConnection());
    }

    /**
     * Implementacion MySQL del Factory Method de la conexion.
     * Toda la dependencia con MySQL (driver, URL, usuario, password) queda
     * encerrada en MySQLConnectionManager y solo esta clase lo conoce.
     */
    @Override
    protected Connection getConnection() {
        return MySQLConnectionManager.getInstance().getConnection();
    }

    /** Cierre especifico de MySQL: delega en su propio gestor de conexiones. */
    @Override
    protected void doShutdown() {
        MySQLConnectionManager.getInstance().shutdown();
    }





}