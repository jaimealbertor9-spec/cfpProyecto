import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

// Modelo de datos para Productos
class Producto {
    String id, nombre; 
    double precio; 
    int cantidadVendida = 0;

    Producto(String id, String n, double p) { 
        this.id = id; 
        this.nombre = n; 
        this.precio = p; 
    }

    public String getId() { 
        return id; 
    }
}

// Modelo de datos para Vendedores
class Vendedor {
    String tipoDoc, numDoc, nombres, apellidos; 
    double ventasTotales = 0.0;

    Vendedor(String td, String nd, String n, String a) { 
        this.tipoDoc = td; 
        this.numDoc = nd; 
        this.nombres = n; 
        this.apellidos = a; 
    }

    public String getNumDoc() { 
        return numDoc; 
    }
}

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("Iniciando procesamiento de reportes...");

            // 1. Cargar catálogo de productos y maestros de vendedores en memoria
            Map<String, Producto> mapaProductos = cargarDatos("productos.csv", linea -> {
                String[] d = linea.split(";"); 
                return new Producto(d[0], d[1], Double.parseDouble(d[2]));
            }, Producto::getId);

            Map<String, Vendedor> mapaVendedores = cargarDatos("vendedores.csv", linea -> {
                String[] d = linea.split(";"); 
                return new Vendedor(d[0], d[1], d[2], d[3]);
            }, Vendedor::getNumDoc);

            // 2. Procesar dinámicamente todos los archivos individuales vendedor_<ID>.csv
            Files.walk(Paths.get("."))
                .filter(path -> path.getFileName().toString().startsWith("vendedor_") && path.toString().endsWith(".csv"))
                .forEach(path -> procesarArchivoVenta(path, mapaProductos, mapaVendedores));

            // 3. Generar y exportar los reportes consolidados ordenados
            generarReportes(mapaVendedores, mapaProductos);

            System.out.println("¡Reportes generados exitosamente!");
        } catch (Exception e) { 
            System.err.println("ERROR EN PROCESAMIENTO: " + e.getMessage()); 
        }
    }

    // Carga genérica de archivos CSV a Mapas en memoria
    private static <T> Map<String, T> cargarDatos(String archivo, java.util.function.Function<String, T> constructor, java.util.function.Function<T, String> getKey) throws IOException {
        return Files.lines(Paths.get(archivo))
                    .map(constructor)
                    .collect(Collectors.toMap(getKey, item -> item));
    }

    // Lectura de ventas por archivo y acumulación de totales
    private static void procesarArchivoVenta(Path archivo, Map<String, Producto> prods, Map<String, Vendedor> vends) {
        try {
            List<String> lineas = Files.readAllLines(archivo);
            String idVendedor = lineas.get(0).split(";")[1];
            Vendedor vendedor = vends.get(idVendedor);
            if (vendedor == null) return;

            for (int i = 1; i < lineas.size(); i++) {
                String[] datos = lineas.get(i).split(";");
                Producto producto = prods.get(datos[0]);
                int cantidad = Integer.parseInt(datos[1]);
                if (producto != null) {
                    vendedor.ventasTotales += producto.precio * cantidad;
                    producto.cantidadVendida += cantidad;
                }
            }
        } catch (Exception e) { 
            System.err.println("ADVERTENCIA en archivo " + archivo.getFileName() + ": " + e.getMessage()); 
        }
    }

    // Exportación de reportes CSV ordenados descendentemente
    private static void generarReportes(Map<String, Vendedor> mapaVendedores, Map<String, Producto> mapaProductos) throws IOException {
        // Reporte de vendedores ordenados por ventasTotales de mayor a menor
        List<Vendedor> vendedoresOrdenados = mapaVendedores.values().stream()
            .sorted(Comparator.comparingDouble((Vendedor v) -> v.ventasTotales).reversed())
            .collect(Collectors.toList());

        try (PrintWriter writer = new PrintWriter("reporte_vendedores.csv", "UTF-8")) {
            for (Vendedor v : vendedoresOrdenados) {
                writer.printf(Locale.US, "%s %s;%.2f\n", v.nombres, v.apellidos, v.ventasTotales);
            }
        }

        // Reporte de productos ordenados por cantidadVendida de mayor a menor
        List<Producto> productosOrdenados = mapaProductos.values().stream()
            .sorted(Comparator.comparingInt((Producto p) -> p.cantidadVendida).reversed())
            .collect(Collectors.toList());

        try (PrintWriter writer = new PrintWriter("reporte_productos.csv", "UTF-8")) {
            for (Producto p : productosOrdenados) {
                writer.printf(Locale.US, "%s;%.2f\n", p.nombre, p.precio);
            }
        }
    }
}