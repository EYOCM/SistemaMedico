// Listas que dependen de otra (sede -> especialidad -> médico -> hora) en las pantallas de recepción.
// Las usan el Walk-in (CU-05 FA04) y la emergencia sin cita (CU-05 FA01).
// FA01 / FA04 / FA06 - CU-05 INICIO
const Selectores = {
    vaciar(select, texto) {
        select.innerHTML = '';
        select.add(new Option(texto, ''));
        select.disabled = true;
    },

    llenar(select, opciones, texto, seleccionado) {
        Selectores.vaciar(select, texto);
        opciones.forEach(o => {
            const valor = o.id !== undefined ? String(o.id) : o;
            const etiqueta = o.nombre !== undefined ? o.nombre : o;
            select.add(new Option(etiqueta, valor, false, String(seleccionado) === valor));
        });
        select.disabled = opciones.length === 0;
    },

    pedir(url, parametros) {
        return fetch(url + '?' + new URLSearchParams(parametros), { headers: { 'Accept': 'application/json' } })
            .then(r => r.ok ? r.json() : [])
            .catch(() => []);
    }
};
// FA01 / FA04 / FA06 - CU-05 FINAL
