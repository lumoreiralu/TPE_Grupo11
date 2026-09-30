package repository;

import dao.FacturaProductoDAO;
import entity.FacturaProducto;

import java.util.*;
import java.sql.*;

public class MySQLFacturaProductoDAO implements FacturaProductoDAO {
    private Connection cn;
    public MySQLFacturaProductoDAO(Connection cn) {
        this.cn = cn;
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        final String sql = "CREATE TABLE IF NOT EXISTS factura_producto (" +
                "idFactura INT, " +
                "idProducto INT, " +
                "cantidad INT NOT NULL, " +
                "PRIMARY KEY (idFactura, idProducto), " +
                "FOREIGN KEY (idFactura) REFERENCES facturas(idFactura) ON DELETE CASCADE, " +
                "FOREIGN KEY (idProducto) REFERENCES productos(idProducto) ON DELETE CASCADE" +
                ")";

        try (Statement st = cn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error creando tabla 'factura_producto'", e);
        }
    }

    @Override
    public FacturaProducto findById(int idFactura, int idProducto) {
        final String sql = "SELECT * FROM factura_producto WHERE idFactura = ? AND idProducto = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idFactura);
            ps.setInt(2, idProducto);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el detalle de la factura", e);
        }
    }

    @Override
    public List<FacturaProducto> findAll() {
        final String sql = "SELECT * FROM factura_producto";
        List<FacturaProducto> out = new ArrayList<>();
        try(PreparedStatement ps = cn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery() ) {
            while(rs.next()) out.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar los detalles de la factura", e);
        }
        return out;
    }


    @Override
    public void create(FacturaProducto producto) {
        final String sql = "INSERT INTO factura_producto (idFactura, idProducto, cantidad) VALUES (?, ?, ?)";
        try(PreparedStatement ps = cn.prepareStatement(sql)){
            ps.setInt(1, producto.getIdFactura());
            ps.setInt(2, producto.getIdProducto());
            ps.setInt(3, producto.getCantidad());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear detalles de la factura", e);
        }

    }

    @Override
    public void update(FacturaProducto producto) {
        final String sql = "UPDATE factura_producto SET cantidad = ? WHERE idFactura = ? AND idProducto = ?";
        try(PreparedStatement ps = cn.prepareStatement(sql)){
            ps.setInt(1, producto.getCantidad());
            ps.executeUpdate();
        }catch (SQLException e) {
            throw new RuntimeException("Error al actualizar los detalles de la factura", e);
        }

    }

    @Override
    public void delete(FacturaProducto producto) {
        final String sql = "DELETE FROM factura_producto WHERE idFactura = ? AND idProducto = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, producto.getIdFactura());
            ps.setInt(2, producto.getIdProducto()); // Asigna el segundo parámetro de la PK compuesta

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar la relación FacturaProducto", e);
        }
    }

    @Override
    public void deleteAll() {
        final String sql = "DELETE FROM factura_producto";
        try (Statement st = cn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando 'detalles de factura'", e);
        }
    }

    // ---- mapper privado ----
    private FacturaProducto map(ResultSet rs) throws SQLException {
        FacturaProducto u = new FacturaProducto();
        u.setIdFactura(rs.getInt("idFactura"));
        u.setIdProducto(rs.getInt("idProducto"));
        u.setCantidad(rs.getInt("cantidad"));
        return u;
    }
}
