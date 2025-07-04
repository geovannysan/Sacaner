package com.tickets.myapplication.Dominio;

import java.util.List;

public class ApiResponse {
        private boolean estado;
        private Boleto boleto;
        private List<Object> comentario;
        private String fechacanje;

        // Getters y setters
        public boolean isEstado() { return estado; }
        public Boleto getBoleto() { return boleto; }
        public List<Object> getComentario() { return comentario; }
        public String getFechacanje() { return fechacanje; }

        public void setEstado(boolean estado) { this.estado = estado; }
        public void setBoleto(Boleto boleto) { this.boleto = boleto; }
        public void setComentario(List<Object> comentario) { this.comentario = comentario; }
        public void setFechacanje(String fechacanje) { this.fechacanje = fechacanje; }

}
