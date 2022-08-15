package io.smallrye.graphql.tests.validation;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

@SuppressWarnings("unused")
public class Person {
    @Pattern(regexp = "\\w+")
    private String firstName;
    @NotEmpty
    private String lastName;
    @PositiveOrZero
    private int age;

    @Pattern(regexp = ".+%")
    private String somethingEndingWithPercentageSign;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getSomethingEndingWithPercentageSign() {
        return somethingEndingWithPercentageSign;
    }

    public void setSomethingEndingWithPercentageSign(String somethingEndingWithPercentageSign) {
        this.somethingEndingWithPercentageSign = somethingEndingWithPercentageSign;
    }
}
