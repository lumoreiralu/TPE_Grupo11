package repository;

import entity.Factura;
import dao.FacturaDAO;
import entity.Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de acceso a datos para la entidad {@link Factura} utilizando MySQL.
 * Provee operaciones CRUD básicas y consultas de agregación.
 *
 *
 * @version 1.0
 */

public class MySQLFacturaDAO implements FacturaDAO {
    private final Connection cn;
    /**
     * Construye una nueva instancia del DAO y crea la tabla si no existe.
     *
     * @param cn Conexión activa a la base de datos MySQL.
     */

    public MySQLFacturaDAO(Connection cn) {
        this.cn = cn;
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {

        final String sql = "CREATE TABLE IF NOT EXISTS facturas ( " +
                "idFactura INT PRIMARY KEY, " +
                "idCliente INT, " +
                "FOREIGN KEY (idCliente) REFERENCES clientes(idCliente) ON DELETE CASCADE" +
                ")";
        try (Statement st = cn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error creando tabla 'facturas'", e);
        }
    }

    @Override
    public Factura findById(Integer id) {
        final String sql = "SELECT * FROM facturas WHERE idFactura = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando la factura con id = " + id, e);
        }
    }

    @Override
    public List<Factura> findAll() {
        final String sql = "SELECT * FROM facturas";
        List<Factura> out = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error en findAll", e);
        }
        return out;
    }

    @Override
    public void create(Factura factura) {

        final String sql = "INSERT INTO facturas (idFactura, idCliente) VALUES(?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, factura.getIdFactura());
            ps.setInt(2, factura.getIdCliente());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error al crear factura", e);
        }
    }

    @Override
    public void update(Factura factura) {
        final String sql = "UPDATE facturas SET idCliente = ? WHERE idFactura = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, factura.getIdCliente());
            ps.setInt(2, factura.getIdFactura());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar factura", e);
        }
    }

    @Override
    public void delete(Factura factura) {
        final String sql = "DELETE FROM facturas WHERE idFactura = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, factura.getIdFactura());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar factura", e);
        }
    }

    @Override
    public void deleteAll() {
        final String sql = "DELETE FROM facturas";
        try (Statement st = cn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando 'facturas'", e);
        }
    }

    // ---- mapper privado ----
    private Factura map(ResultSet rs) throws SQLException {
        Factura u = new Factura();
        u.setIdFactura(rs.getInt("idFactura"));
        u.setIdCliente(rs.getInt("idCliente"));
        return u;
    }
}