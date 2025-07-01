import random


def luhn_checksum(card_number):
    def digits_of(n):
        return [int(d) for d in str(n)]
    digits = digits_of(card_number)
    checksum = 0
    odd = len(digits) % 2 == 0
    for i, d in enumerate(digits):
        if i % 2 == 0 if odd else i % 2 == 1:
            d = d * 2
            if d > 9:
                d -= 9
        checksum += d
    return checksum % 10


def generate_valid_pan(bin_prefix: str, length: int = 16):
    if len(bin_prefix) >= length:
        raise ValueError("BIN quá dài so với độ dài PAN")

    num_random_digits = length - len(bin_prefix) - 1
    pan = bin_prefix + ''.join(str(random.randint(0, 9))
                               for _ in range(num_random_digits))

    for check_digit in range(10):
        test_pan = pan + str(check_digit)
        if luhn_checksum(test_pan) == 0:
            return test_pan

    raise RuntimeError("Không thể tính được check digit hợp lệ")

if __name__ == "__main__":
    bin_code = "970426"
    for _ in range(1):
        print(generate_valid_pan(bin_code))
