package utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import dao.ClienteDAO;
import dao.ProductoDAO;
import dao.FacturaDAO;
import dao.FacturaProductoDAO;

import entity.Cliente;
import entity.Producto;
import entity.Factura;
import entity.FacturaProducto;

import factory.DAOFactory;

public class CargarDatosIniciales {

    private final ClienteDAO clienteDAO;
    private final ProductoDAO productoDAO;
    private final FacturaDAO facturaDAO;
    private final FacturaProductoDAO facturaProductoDAO;

    public CargarDatosIniciales() {
        DAOFactory f = DAOFactory.getInstance();
        this.clienteDAO       = f.createClienteDAO();
        this.productoDAO       = f.createProductoDAO();
        this.facturaDAO         = f.createFacturaDAO();
        this.facturaProductoDAO = f.createFacturaProductoDAO();
    }

    public void run() {
        // Orden de carga respetando Foreign Keys
        cargarClientes("/data/clientes.csv");
        cargarProductos("/data/productos.csv");
        cargarFacturas("/data/facturas.csv");
        cargarFacturasProductos("/data/facturas-productos.csv");
    }

    private void cargarClientes(String resourcePath) {
        try (InputStream is = mustGetResource(resourcePath);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

            for (CSVRecord row : parser) {
                int idCliente = Integer.parseInt(row.get("idCliente"));
                String nombre = row.get("nombre");
                String email  = row.get("email");

                clienteDAO.create(new Cliente(idCliente, nombre, email));
            }
            System.out.println("Clientes cargados OK.");
        } catch (Exception e) {
            throw new RuntimeException("Error cargando clientes desde " + resourcePath, e);
        }
    }

    private void cargarProductos(String resourcePath) {
        try (InputStream is = mustGetResource(resourcePath);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

            for (CSVRecord row : parser) {
                int idProducto = Integer.parseInt(row.get("idProducto"));
                String nombre   = row.get("nombre");
                float valor     = Float.parseFloat(row.get("valor"));

                productoDAO.create(new Producto(idProducto, nombre, valor));
            }
            System.out.println("Productos cargados OK.");
        } catch (Exception e) {
            throw new RuntimeException("Error cargando productos desde " + resourcePath, e);
        }
    }

    private void cargarFacturas(String resourcePath) {
        try (InputStream is = mustGetResource(resourcePath);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

            for (CSVRecord row : parser) {
                int idFactura = Integer.parseInt(row.get("idFactura"));
                int idCliente = Integer.parseInt(row.get("idCliente"));

                facturaDAO.create(new Factura(idFactura, idCliente));
            }
            System.out.println("Facturas cargadas OK.");
        } catch (Exception e) {
            throw new RuntimeException("Error cargando facturas desde " + resourcePath, e);
        }
    }

    private void cargarFacturasProductos(String resourcePath) {
        try (InputStream is = mustGetResource(resourcePath);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

            for (CSVRecord row : parser) {
                int idFactura  = Integer.parseInt(row.get("idFactura"));
                int idProducto = Integer.parseInt(row.get("idProducto"));
                int cantidad   = Integer.parseInt(row.get("cantidad"));

                facturaProductoDAO.create(new FacturaProducto(idFactura, idProducto, cantidad));
            }
            System.out.println("FacturaProducto cargados OK.");
        } catch (Exception e) {
            throw new RuntimeException("Error cargando factura-producto desde " + resourcePath, e);
        }
    }

    // --- util ---
    private InputStream mustGetResource(String path) {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) throw new IllegalArgumentException("Recurso no encontrado: " + path);
        return is;
    }
}