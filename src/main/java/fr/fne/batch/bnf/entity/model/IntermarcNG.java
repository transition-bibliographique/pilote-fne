package fr.fne.batch.bnf.entity.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe repésentant un intermarcNG
 * Un intermarc est composé d'une list de {@link ZoneI}
 * 
 * @author Mickael KWINE KWOR MAN
 * @author Thibaud SENALADA
 */
public class IntermarcNG {

	private List<ZoneI> zones;

	/**
	 * Constructeur
	 */
	public IntermarcNG() {
		this.zones = new ArrayList<>();
	}

	public List<ZoneI> getZones() {
		return this.zones;
	}

	public void setZones(List<ZoneI> zones) {
		this.zones = zones;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.zones.isEmpty() ? 0 : this.zones.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		IntermarcNG other = (IntermarcNG) obj;
		if (this.zones.isEmpty()) {
			if (!other.zones.isEmpty()) {
				return false;
			}
		} else {
			if (!this.zones.equals(other.zones)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Ajoute une zone a l'intermarc
	 *
	 * @param zone
	 *            {@link ZoneI} zone à ajouter
	 */
	public void addZoneI(ZoneI zone) {
		this.zones.add(zone);
	}

	public void addZoneI(ZoneI zone, int index) {
		this.zones.add(index, zone);
	}
	
	
	@Override
	public String toString() {
		return "Intermarc [zones=" + this.zones + "]";
	}

	
}
