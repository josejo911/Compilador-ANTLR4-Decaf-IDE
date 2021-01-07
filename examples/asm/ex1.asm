.text
.globl main
main:
    li $s0, 2
    sw $s0, b_main                # str data
    li $s1, 2
    sw $s1, c_main                # str data
    li $s2, 9
    sw $s2, d_main                # str data
    lw $t1, c_main                # ld data c_main
    lw $t2, d_main                # ld data d_main
    mult $t1, $t2
    mflo $t0
    lw $t2, b_main                # ld data b_main
    add $s3, $t2, $t0
    sw $s3, a_main                # str data
    li $v0, 1                    # Print call
    lw $a0, d_main
    syscall
    lw $t0, d_main                # ld data d_main
    move $t1, $t0
    lw $t2, a_main                # ld data a_main
    lw $t0, b_main                # ld data b_main
    mult $t2, $t0
    mflo $s4
    sw $s4, d_main                # str data


    # ---------- Exit ----------
    li $v0, 10
    syscall


# ---------- data section ----------
.data
a_main:                .word 0
b_main:                .word 0
c_main:                .word 0
d_main:                .word 0