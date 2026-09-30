
import dao.ClienteDAO;
import dto.ProductoRecaudadoDTO;
import entity.Cliente;
import dao.FacturaProductoDAO;
import dao.ProductoDAO;
import dao.FacturaDAO;
import entity.Factura;
import entity.Producto;
import factory.DAOFactory;
import factory.DBType;
import utils.BorrarDatos;
import utils.CargarDatosIniciales;


public class Main {

    /**
     * ======== ÚNICO PUNTO DE SWITCHEO ENTRE MOTORES ========

     * Cambiá DBType.DERBY por DBType.MYSQL (o al revés) y TODA la aplicación
     * pasa a usar la otra base. No hay que tocar ninguna otra línea:
     * ni Main, ni BorrarDatos, ni CargarDatosIniciales, ni los DAO.

     * Eso es lo que compra el Abstract Factory.
     */
    private static final DBType MOTOR = DBType.MYSQL;

    public static void main(String[] args) {

        /*
         * ESTO TIENE QUE SER LO PRIMERO DE TODO.
         *
         * DAOFactory es un Singleton: el PRIMER getInstance() del proceso decide el motor
         * y los siguientes devuelven esa misma fábrica, ignorando lo que se les pase.
         * Como new BorrarDatos() ya pide la fábrica, si dejáramos el switcheo más abajo
         * llegaría tarde y la aplicación seguiría corriendo contra el motor por defecto.
         *
         * Publicamos el motor elegido como system property para que TODOS los que llamen
         * a DAOFactory.getInstance() sin parámetro resuelvan lo mismo.
        *
         * Si además se pasa -Ddb.type=MYSQL por línea de comandos, ese valor gana:
         * permite switchear sin recompilar.
         */
        System.setProperty("db.type", System.getProperty("db.type", MOTOR.name()));
        System.out.println("=== Motor de base de datos: " + System.getProperty("db.type") + " ===");

        new BorrarDatos().run();
        System.out.println("Listo.");

        new CargarDatosIniciales().run();
        System.out.println("Carga inicial finalizada.");


        DAOFactory f = DAOFactory.getInstance(); // resuelve segun db.type, ya fijado arriba

        ClienteDAO clienteDAO = f.createClienteDAO();
        ProductoDAO productoDAO = f.createProductoDAO();
        FacturaDAO facturaDAO = f.createFacturaDAO();
        FacturaProductoDAO facturaProductoDAO = f.createFacturaProductoDAO();

        ProductoRecaudadoDTO productoRecaudado =productoDAO.findTopRevenueProduct();

        // 5. Ejemplo para probar el funcionamiento del sistema
        try {
            System.out.println("\n--- PRUEBA DE CONSULTAS ---");
            Producto pr1 =  productoDAO.findById(1);
            Cliente cl1 =  clienteDAO.findById(1);
            Factura fa1 =  facturaDAO.findById(1);
            System.out.println("Producto 1: " + pr1 + " Cliente: " + cl1 + " Factura: " + fa1);

            Producto p1 =  productoDAO.findById(16);
            System.out.println("Producto 16: " + p1.getNombre() );
            p1.setNombre("Prueba");
            System.out.println("Nuevo nombre: " + p1.getNombre());

            Cliente cl12 =  clienteDAO.findById(12);
            System.out.println("Cliente 12: " + cl12.getNombre());
            cl12.setNombre("Prueba");
            System.out.println("Nuevo nombre: " + cl12.getNombre());

            Producto nuevo = new Producto();
            nuevo.setNombre("1000");
            nuevo.setNombre("Prueba 1111");
            nuevo.setValor(200);
            productoDAO.create(nuevo);
            Producto guardado =  productoDAO.findById(1000);
            System.out.println("Producto nuevo: " + nuevo.getNombre());

            if (productoRecaudado != null) {
                System.out.println("Producto con mayor recaudación:");
                System.out.println("ID: " + productoRecaudado.getIdProducto());
                System.out.println("Nombre: " + productoRecaudado.getNombre());
                System.out.println("Valor: " + productoRecaudado.getValor());
                System.out.println("Recaudación: " + productoRecaudado.getRecaudacion());
            }

            Factura fa2 =  facturaDAO.findById(11);
            System.out.println("Factura fa2 tiene como cliente a: " + fa2.getIdCliente());
            fa2.setIdCliente(10);
            System.out.println("Factura fa2 ahora tiene como cliente a: " + fa2.getIdCliente());

        } catch (Exception e) {
            System.err.println("Error ejecutando consultas de prueba: " + e.getMessage());
            e.printStackTrace();
        }

        // Cierre de la base. Polimorfico: Main no sabe que motor hay debajo.
        // Con Derby esto es OBLIGATORIO (si no, la base queda en estado inconsistente).
        f.shutdown();



    }
}
