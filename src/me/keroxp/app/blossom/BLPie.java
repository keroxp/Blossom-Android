package me.keroxp.app.blossom;

public class BLPie {
	BLPiePiece[] pieces;
	public BLPie(String key) {
		// TODO Auto-generated constructor stub
	}
}

// パイメニューに表示するオブジェクト

class BLPiePiece {
	int index;
	String key;
	public BLPiePiece(String key, int index){
		this.index = index;
		this.key = key;
	}
}