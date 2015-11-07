package net.filebot.ant.spk.openpgp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.security.SignatureException;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

public class OpenPGPSignature {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	private PGPSignatureGenerator signature;

	public OpenPGPSignature(OpenPGPSecretKey key) throws PGPException {
		PGPDigestCalculatorProvider pgpDigestCalculator = new JcaPGPDigestCalculatorProviderBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build();
		PBESecretKeyDecryptor pbeSecretKeyDecryptor = new JcePBESecretKeyDecryptorBuilder(pgpDigestCalculator).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(key.getPassword());
		JcaPGPContentSignerBuilder pgpContentSigner = new JcaPGPContentSignerBuilder(key.getSecretKey().getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1).setProvider(BouncyCastleProvider.PROVIDER_NAME).setDigestProvider(BouncyCastleProvider.PROVIDER_NAME);

		signature = new PGPSignatureGenerator(pgpContentSigner);

		PGPPrivateKey privateKey = key.getSecretKey().extractPrivateKey(pbeSecretKeyDecryptor);
		signature.init(PGPSignature.BINARY_DOCUMENT, privateKey);
	}

	public void update(byte[] buffer, int offset, int length) throws SignatureException {
		signature.update(buffer, offset, length);
	}

	public void generate(OutputStream output, boolean asciiArmor) throws IOException, SignatureException, PGPException {
		if (asciiArmor) {
			output = new ArmoredOutputStream(output);
		}
		signature.generate().encode(new BCPGOutputStream(output));
	}

	public byte[] generate(boolean asciiArmor) throws IOException, SignatureException, PGPException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		generate(out, asciiArmor);
		return out.toByteArray();
	}

	public static OpenPGPSignature createSignatureGenerator(String keyId, File secring, char[] password) throws FileNotFoundException, IOException, PGPException {
		try (InputStream secretKeyRing = new FileInputStream(secring)) {
			OpenPGPSecretKey key = new OpenPGPSecretKey(keyId, secretKeyRing, password);
			OpenPGPSignature signature = new OpenPGPSignature(key);
			return signature;
		}
	}

}
