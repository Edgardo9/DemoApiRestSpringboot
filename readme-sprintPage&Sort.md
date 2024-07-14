# Pasos de ayuda sobre Spring "Page & sort"

## 01 - Extender "PagingAndSortingRepository" en Repository.
Se debe extender en la clase "repository", la interface "PagingAndSortingRepository". Si ya se esta utilizando la interface "CrudREpository", se puede ampliar agregando esta nueva interface. Quedando de la siguiente forma:
    public interface UsuarioRepository extends CrudRepository<UsuarioClass, Long>, PagingAndSortingRepository<UsuarioClass, Long> {...}

## 02 - Aplicando paginacion en el controlador
Entendiendo los parametros de la Paginacion en el controlador (En el caso de la estreuctura de este proyecto, deberia ser desde la implementacion del @Servicio):

    @GetMapping
    private ResponseEntity<List<CashCard>> findAll(Pageable pageable) {
        Page<CashCard> page = cashCardRepository.findAll(
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize()
        ));
        return ResponseEntity.ok(page.getContent());
    }

* En el metodo del controlador donde se obtendra el listado y se ordenara, se debe pasar como parametro de entrada un objeto de tipo "Pageable". "Pageable" es otro objeto mas que Spring Web nos proporciona. Dado que especificamos los parametros de URI de "page=0&size=1", "pageable" contendra los valores que necesitamos.

* PageRequest es una implementación básica de Java Bean de Pageable. Las cosas que requieren implementación de paginación y clasificación a menudo admiten esto, como algunos tipos de Spring Data Repositories.

## 03 - Ordenando:
para agregar orden desde el controlador, solo se debe agregar un parametro adicional en la llamada de "PageRequest.of()":
    PageRequest.of(
        pageable.getPageNumber(),
        pageable.getPageSize(),
        pageable.getSort()
    );

* El método getSort() extrae el parámetro de consulta de clasificación del URI de solicitud.

## 04 - Paginacion y ordenamiento por defecto:
Hasta ahora tenemos un endpoint que requiere que el cliente envíe cuatro datos: el índice y el tamaño de la página, el orden de clasificación y la dirección. Esto es mucho pedir, así que hagámoslo más fácil para ellos.

* Para agregar un ordenamiento por defecto, se debe modificar el metodo "getSort()" por "getSortOr()", y se debe pasar el valor por el cual se requiera el ordenamiento:
    pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))

* El método getSortOr() proporciona valores predeterminados para los parámetros de página, tamaño y clasificación. Los valores predeterminados provienen de dos fuentes diferentes: Spring proporciona los valores predeterminados de página y tamaño (son 0 y 20, respectivamente). 
* Nuevamente: no necesitábamos definir explícitamente estos valores predeterminados. Spring los proporciona "listos para usar".

