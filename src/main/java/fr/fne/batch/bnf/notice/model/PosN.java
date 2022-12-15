package fr.fne.batch.bnf.notice.model;

import java.util.Objects;

public class PosN extends SousElementN {
	
	private String sens;

	public PosN(String code, String sens) {
		super(code);
		this.sens = sens;
	}
	
	public String getSens() {
		return sens;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(sens);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof PosN)) {
			return false;
		}
		PosN other = (PosN) obj;
		return Objects.equals(sens, other.sens);
	}

	
}
