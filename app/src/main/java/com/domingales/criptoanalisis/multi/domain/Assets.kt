package com.domingales.criptoanalisis.multi.domain

object Assets {
    val supported = listOf(
        CryptoAsset("BTC", "Bitcoin"), CryptoAsset("ETH", "Ethereum"), CryptoAsset("BNB", "BNB"),
        CryptoAsset("SOL", "Solana"), CryptoAsset("ADA", "Cardano"), CryptoAsset("XRP", "XRP"),
        CryptoAsset("DOGE", "Dogecoin"), CryptoAsset("DOT", "Polkadot"), CryptoAsset("AVAX", "Avalanche"),
        CryptoAsset("LINK", "Chainlink"), CryptoAsset("TRX", "TRON"), CryptoAsset("LTC", "Litecoin"),
        CryptoAsset("BCH", "Bitcoin Cash"), CryptoAsset("ATOM", "Cosmos"), CryptoAsset("UNI", "Uniswap"),
        CryptoAsset("ETC", "Ethereum Classic"), CryptoAsset("FIL", "Filecoin"), CryptoAsset("NEAR", "NEAR"),
        CryptoAsset("APT", "Aptos"), CryptoAsset("ARB", "Arbitrum"), CryptoAsset("OP", "Optimism"),
        CryptoAsset("SUI", "Sui"), CryptoAsset("INJ", "Injective"), CryptoAsset("AAVE", "Aave"),
        CryptoAsset("ALGO", "Algorand"), CryptoAsset("VET", "VeChain"), CryptoAsset("ICP", "Internet Computer"),
        CryptoAsset("GRT", "The Graph"), CryptoAsset("MKR", "Maker"), CryptoAsset("RUNE", "THORChain")
    )
}
