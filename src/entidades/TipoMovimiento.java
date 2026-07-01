package entidades;

public enum TipoMovimiento {
	TRANSFERENCIA_ENVIADA {
		@Override public int signo() { return -1; }
	},
	TRANSFERENCIA_RECIBIDA {
		@Override public int signo() { return 1; }
	},
	DEBITO_TARJETA {
		@Override public int signo() { return -1; }
	},
	PAGO_TARJETA {
		@Override public int signo() { return 1; }
	},
	CHEQUE_COBRADO {
		@Override public int signo() { return -1; }
	};

	public abstract int signo();
}
