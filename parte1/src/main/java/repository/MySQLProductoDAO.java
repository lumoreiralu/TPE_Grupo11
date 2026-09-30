package repository;
import dao.ProductoDAO;
import dto.ProductoRecaudadoDTO;
import entity.Producto;

import java.sql.*;
import java.util.*;
/**
 * Implementación de acceso a datos para la entidad {@link Producto} utilizando MySQL.
 * Provee operaciones CRUD básicas y consultas de agregación.
 *
 *
 * @version 1.0
 */

public class MySQLProductoDAO implements ProductoDAO {

    private final Connection cn;

    /**
     * Construye una nueva instancia del DAO y crea la tabla si no existe.
     *
     * @param cn Conexión activa a la base de datos MySQL.
     */

    public MySQLProductoDAO(Connection cn) {
        this.cn = cn;
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        final String sql = "CREATE TABLE IF NOT EXISTS productos (" +
                "idProducto INT PRIMARY KEY," +
                "nombre VARCHAR(100) NOT NULL," +
                "valor FLOAT NULL" +
                ")";
        try (Statement st = cn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error creando tabla 'productos'", e);
        }
    }

    /**
     * Busca un producto por su clave primaria.
     *
     * @param id Identificador único del producto.
     * @return El {@link Producto} encontrado, o {@code null} si no existe.
     * @throws RuntimeException Si ocurre un error de acceso a datos (SQLException).
     */


    @Override
    public Producto findById(int id) {
        final String sql = "SELECT * FROM productos WHERE idProducto = ?";
        try(PreparedStatement ps = cn.prepareStatement(sql)){
            ps.setInt(1, id);
            try(ResultSet rs = ps.executeQuery()){
                return rs.next() ? map(rs) : null ;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar producto por id" + id, e);
        }
    }

    @Override
    public List<Producto> findAll() {
        final String sql = "SELECT * FROM productos";
        List<Producto> out = new ArrayList<>();
        try(PreparedStatement ps = cn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()){
                while(rs.next()) out.add(map(rs));
        } catch(SQLException e) {
            throw new RuntimeException("Error al buscar todos los productos", e);
        }
        return out;
    }

    @Override
    public void create(Producto producto) {
        final String sql = "INSERT INTO productos (idProducto, nombre, valor) VALUES (?, ?, ?)";
        try(PreparedStatement ps = cn.prepareStatement(sql)){
            ps.setInt(1,producto.getIdProducto());
            ps.setString(2, producto.getNombre());
            ps.setFloat(3, producto.getValor());
            ps.executeUpdate();
        }catch(SQLException e) {
            throw new RuntimeException("Error al insertar producto", e);
        }

    }

    @Override
    public void update(Producto producto) {
        final String sql = "UPDATE productos SET nombre = ?, valor = ? WHERE idProducto = ?";
        try(PreparedStatement ps = cn.prepareStatement(sql)){
            ps.setString(1, producto.getNombre());
            ps.setFloat(2, producto.getValor());
            ps.setInt(3, producto.getIdProducto());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al modificar el producto", e);
        }

    }

    @Override
    public void delete(Producto producto) {
        final String sql = "DELETE FROM productos WHERE idProducto = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, producto.getIdProducto());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error en delete", e);
        }
    }

    @Override
    public void deleteAll() {
        final String sql = "DELETE FROM productos";
        try (Statement st = cn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando 'productos'", e);
        }
    }

    /**
     * Obtiene el producto que generó la mayor recaudación histórica acumulada.
     *
     * @return Objeto {@link ProductoRecaudadoDTO } con los datos del producto y el total facturado,
     *         o {@code null} si no hay registros asociados.
     * @throws RuntimeException Si falla la consulta SQL.
     */


    @Override
    public ProductoRecaudadoDTO findTopRevenueProduct() {
        final String sql = "SELECT p.idProducto, p.nombre, p.valor, SUM(fp.cantidad * p.valor) as revenue " +
                "FROM productos p " +
                "JOIN factura_producto fp ON p.idProducto = fp.idProducto " +
                "GROUP BY p.idProducto, p.nombre, p.valor " +
                "ORDER BY revenue DESC " +
                "LIMIT 1";
        try (PreparedStatement ps = cn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new ProductoRecaudadoDTO(
                                rs.getInt("idProducto"),
                                rs.getString("nombre"),
                                rs.getFloat("valor"),
                                rs.getFloat("revenue")
                        );
                    } else {
                        return null; // No hay productos
                    }
                }
        catch (SQLException e) {
            throw new RuntimeException("Error al buscar el producto con mayor ingreso", e);
        }
    }
            
        

    // ---- mapper privado ----
    /**
     * Transforma el registro actual del {@link ResultSet} en una instancia de {@link Producto}.
     *
     * @param rs El conjunto de resultados posicionado en la fila a mapear.
     * @return Una nueva instancia de {@link Producto} con los datos de la fila.
     * @throws SQLException Si ocurre un error al acceder a las columnas en la base de datos.
     */
    private Producto map(ResultSet rs) throws SQLException {
        Producto u = new Producto();
        u.setIdProducto(rs.getInt("idProducto"));
        u.setNombre(rs.getString("nombre"));
        u.setValor(rs.getFloat("valor"));
        return u;
    }
}
