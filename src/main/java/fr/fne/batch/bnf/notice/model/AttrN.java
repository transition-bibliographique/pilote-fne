package fr.fne.batch.bnf.notice.model;

import java.util.Objects;

public class AttrN {

	private String nom;
		
	private String rang;
	
	private String rep;

	private String value;
	
	private String sens;
	
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getRang() {
		return rang;
	}

	public void setRang(String rang) {
		this.rang = rang;
	}

	public String getRep() {
		return rep;
	}

	public void setRep(String rep) {
		this.rep = rep;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return the sens
	 */
	public String getSens() {
		return sens;
	}

	/**
	 * @param sens the sens to set
	 */
	public void setSens(String sens) {
		this.sens = sens;
	}

	@Override
	public int hashCode() {
		return Objects.hash(nom, rang, rep, sens, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof AttrN)) {
			return false;
		}
		AttrN other = (AttrN) obj;
		return Objects.equals(nom, other.nom) && Objects.equals(rang, other.rang) && Objects.equals(rep, other.rep)
				&& Objects.equals(sens, other.sens) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "AttrN [nom=" + nom + ", rang=" + rang + ", rep=" + rep + ", value=" + value + ", sens=" + sens + "]";
	}

	
}
