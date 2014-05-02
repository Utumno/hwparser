package gr.uoa.di.hw.helpers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class SmithNumbers {

	private SmithNumbers() {}

	public static void smiths(String dirname, String filename)
			throws IOException {
		final Path path = Paths.get(dirname, filename);
		List<String> readAllLines = Files.readAllLines(path,
			StandardCharsets.ISO_8859_1);
		int nonSmiths = 0;
		int smiths = 0;
		for (String line : readAllLines) {
			String[] split = line.trim().split(" ");
			if (split.length > 0) {
				final String s = split[0];
				try {
					Integer smith = Integer.valueOf(s);
					if (!checkSmith(smith)) ++nonSmiths;
					else ++smiths;
				} catch (NumberFormatException e) {
					// log("first word is :" + s);
				}
			}
		}
		System.out.println(path.getName(path.getNameCount() - 2)
			+ "\n\tNOT smiths : " + nonSmiths + "\n\tsmiths " + smiths);
	}

	private static boolean checkSmith(int smith) {
		return smith != 1 && (sumDig(smith) == sumFactors(smith));
	}

	private static int sumDig(int n) {
		int s = 0;
		while (n > 0) {
			s += (n % 10);
			n = n / 10;
		}
		return s;
	}

	/**
	 * Adds the sum of the factors for the number - except if the number is
	 * prime (or one) so it returns 0
	 */
	private static int sumFactors(final int sm) {
		int num = sm;
		if (num == 1 || num == 2) return 0;
		int factor = 2;
		int sum = 0;
		while (num % factor == 0) {
			num /= factor;
			sum += factor;
		}
		factor = 3;
		int factorSumDigits = sumDig(factor);
		int fact2 = factor * factor;
		while (fact2 <= num) {
			if (num % factor == 0) {
				num /= factor;
				sum += factorSumDigits;
			} else {
				factor += 2;
				factorSumDigits = sumDig(factor);
				fact2 = factor * factor;
			}
		}
		if (num != 1 && num != sm) {
			sum += sumDig(num);
		}
		return sum;
	}
}
