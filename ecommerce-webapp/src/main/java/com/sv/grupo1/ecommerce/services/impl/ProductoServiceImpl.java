package com.sv.grupo1.ecommerce.services.impl;

import com.sv.grupo1.backoffice.service.ProductoImagenStorageService;
import com.sv.grupo1.ecommerce.dao.CategoriaRepository;
import com.sv.grupo1.ecommerce.dao.MarcaRepository;
import com.sv.grupo1.ecommerce.dao.ProductoRepository;
import com.sv.grupo1.ecommerce.entities.catalogo.Marca;
import com.sv.grupo1.ecommerce.entities.catalogo.Categoria;
import com.sv.grupo1.ecommerce.entities.core.Producto;
import com.sv.grupo1.ecommerce.services.ProductoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final ProductoImagenStorageService imagenStorageService;

    public ProductoServiceImpl(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,
            MarcaRepository marcaRepository,
            ProductoImagenStorageService imagenStorageService) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.imagenStorageService = imagenStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarProductos() {
        List<Producto> productos = productoRepository.findAllConRelaciones();
        productos.forEach(imagenStorageService::cargarImagenesEnProducto);
        return productos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> filtrarProductos(String busqueda, Integer idCategoria, Integer idMarca) {

        List<Producto> todos = productoRepository.findAllConRelaciones();

        String termino = (busqueda != null && !busqueda.isBlank())
                ? normalizarTexto(busqueda)
                : null;

        List<Producto> filtrados = todos.stream()

                .filter(p -> Character.valueOf('A').equals(p.getEstado()))

                .filter(p -> {

                    if (termino == null) {
                        return true;
                    }

                    boolean coincideSku = normalizarTexto(p.getCodigoSku()).contains(termino);

                    boolean coincideNombre = normalizarTexto(p.getNombreProducto()).contains(termino);

                    boolean coincideDescripcion = normalizarTexto(p.getDescripcionProducto()).contains(termino);

                    boolean coincideMarca = p.getMarca() != null
                            && normalizarTexto(p.getMarca().getNombreMarca()).contains(termino);

                    boolean coincideCategoria = false;

                    Categoria categoriaActual = p.getCategoria();

                    while (categoriaActual != null) {

                        if (normalizarTexto(categoriaActual.getNombreCategoria()).contains(termino)) {
                            coincideCategoria = true;
                            break;
                        }

                        categoriaActual = categoriaActual.getCategoriaPadre();
                    }

                    return coincideSku
                            || coincideNombre
                            || coincideDescripcion
                            || coincideMarca
                            || coincideCategoria;
                })

                .filter(p -> idCategoria == null
                        || (p.getCategoria() != null
                        && p.getCategoria().perteneceAJerarquia(idCategoria)))

                .filter(p -> idMarca == null
                        || (p.getMarca() != null
                        && idMarca.equals(p.getMarca().getIdMarca())))

                .toList();

        filtrados.forEach(imagenStorageService::cargarImagenesEnProducto);

        return filtrados;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Producto> obtenerPorId(Integer idProducto) {
        return productoRepository.findByIdConRelaciones(idProducto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAllByOrderByNombreCategoriaAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Marca> listarMarcas() {
        return marcaRepository.findAllByOrderByNombreMarcaAsc();
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

}