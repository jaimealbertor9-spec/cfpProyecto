import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

/**
 * Clase responsable de la generacion sintetica y pseudoaleatoria de archivos
 * de prueba para el sistema de ventas.
 * 
 * Satisface los requerimientos de la primera clase con metodo main para la
 * Entrega Final del modulo Conceptos Fundamentales de Programacion.
 * 
 * @author Jaime Alberto Rodriguez Jaramillo
 * @version 3.0
 */
public class GenerateInfoFiles {

    private static final String[] NOMBRES = {
        "Carlos", "Ana", "Luis", "Maria", "Juan", "Sofia", "David", "Laura", "Andres", "Camila"
    };
    
    private static final String[] APELLIDOS = {
        "Gomez", "Perez", "Rodriguez", "Martinez", "Lopez", "Garcia", "Torres", "Ramirez", "Sanchez"
    };
    
    private static final String[] TIPOS_DOC = {"CC", "CE", "TI"};
    
    private static final String[] PRODUCTOS_BASE = {
        "Laptop", "Mouse", "Teclado", "Monitor", "Diadema", "Impresora", "Disco SSD", "Memoria RAM", "Webcam"
    };

    /**
     * Metodo principal de ejecucion. Genera los catalogos maestros y las ventas
     * sin requerir interaccion con el usuario.
     * 
     * @param args Argumentos de linea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        try {
            System.out.println("Iniciando generacion pseudoaleatoria de archivos...");

            // 1. Genera catalogo maestro con 10 productos
            createProductsFile(10);

            // 2. Genera catalogo maestro con 5 vendedores y crea sus archivos individuales de ventas
            createSalesManInfoFile(5);

            System.out.println("Archivos generados exitosamente!");
        } catch (Exception e) {
            System.err.println("ERROR durante la generacion de archivos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Crea un archivo plano con informacion pseudoaleatoria de productos.
     * Formato por linea: ID Producto; Nombre Producto; Precio Por Unidad Producto
     * 
     * @param productsCount Cantidad total de productos a generar en productos.csv.
     * @throws Exception Si ocurre un error de escritura en disco.
     */
    public static void createProductsFile(int productsCount) throws Exception {
        Random rand = new Random();
        try (PrintWriter writer = new PrintWriter("productos.csv", StandardCharsets.UTF_8.name())) {
            for (int i = 1; i <= productsCount; i++) {
                // Generacion modular para admitir cualquier cantidad sin desbordamiento de indice
                String base = PRODUCTOS_BASE[(i - 1) % PRODUCTOS_BASE.length];
                String nombre = base + " Mod_" + i;
                // Rango de precios coherentes entre 15,000.00 y 1,500,000.00
                double precio = 15000.0 + (rand.nextDouble() * 1485000.0);
                
                writer.printf(Locale.US, "%d;%s;%.2f\n", i, nombre, precio);
            }
        }
    }

    /**
     * Crea un archivo plano con informacion de vendedores y coordina la creacion
     * individual de los archivos de ventas por cada asesor comercial.
     * Formato por linea: Tipo Documento; Numero Documento; Nombres; Apellidos
     * 
     * @param salesmanCount Numero total de vendedores a generar en vendedores.csv.
     * @throws Exception Si ocurre un error durante el procesamiento de archivos.
     */
    public static void createSalesManInfoFile(int salesmanCount) throws Exception {
        Random rand = new Random();
        try (PrintWriter writer = new PrintWriter("vendedores.csv", StandardCharsets.UTF_8.name())) {
            for (int i = 0; i < salesmanCount; i++) {
                String tipoDoc = TIPOS_DOC[rand.nextInt(TIPOS_DOC.length)];
                long numDoc = 100000000L + rand.nextInt(900000000);
                String nombre = NOMBRES[rand.nextInt(NOMBRES.length)];
                String apellido = APELLIDOS[rand.nextInt(APELLIDOS.length)];

                writer.printf("%s;%d;%s;%s\n", tipoDoc, numDoc, nombre, apellido);

                // Genera entre 3 y 12 ventas para el archivo particular de este vendedor
                int randomSalesCount = 3 + rand.nextInt(10);
                createSalesMenFile(randomSalesCount, nombre + " " + apellido, numDoc);
            }
        }
    }

    /**
     * Crea un archivo plano individual de ventas para un vendedor en particular.
     * Formato linea 1: Tipo Documento Vendedor; Numero Documento Vendedor
     * Formato transacciones subsiguientes: ID Producto; Cantidad Producto Vendido;
     * 
     * @param randomSalesCount Cantidad de ventas aleatorias a registrar.
     * @param name             Nombre descriptivo del vendedor.
     * @param id               Numero de documento unico del vendedor.
     * @throws Exception       Si ocurre un fallo en la escritura en disco.
     */
    public static void createSalesMenFile(int randomSalesCount, String name, long id) throws Exception {
        Random rand = new Random();
        String filename = "vendedor_" + id + ".csv";
        try (PrintWriter writer = new PrintWriter(filename, StandardCharsets.UTF_8.name())) {
            // Cabecera obligatoria de identificación
            writer.printf("CC;%d\n", id);

            for (int i = 0; i < randomSalesCount; i++) {
                // Selecciona IDs de productos dentro del rango generado (1 al 10)
                int idProducto = 1 + rand.nextInt(10);
                int cantidad = 1 + rand.nextInt(7);
                writer.printf("%d;%d;\n", idProducto, cantidad);
            }
        }
    }
}