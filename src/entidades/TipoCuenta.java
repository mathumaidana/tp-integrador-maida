package entidades;

public enum TipoCuenta {
	CAJA_AHORRO_PESOS(Moneda.PESOS) {
		@Override
		public Cuenta nuevaCuenta() {
			return new CajaAhorro(this);
		}
	},
	CAJA_AHORRO_DOLARES(Moneda.DOLARES) {
		@Override
		public Cuenta nuevaCuenta() {
			return new CajaAhorro(this);
		}
	},
	CUENTA_CORRIENTE(Moneda.PESOS) {
		@Override
		public Cuenta nuevaCuenta() {
			return new CuentaCorriente(this);
		}
	};

	private final Moneda moneda;

	TipoCuenta(Moneda moneda) {
		this.moneda = moneda;
	}

	public Moneda getMoneda() {
		return moneda;
	}

	public abstract Cuenta nuevaCuenta();
}
