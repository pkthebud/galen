/*******************************************************************************
* Copyright 2015 Ivan Shubin http://mindengine.net
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************************/
package net.mindengine.galen.tests.parser;

import static net.mindengine.galen.components.TestUtils.deleteSystemProperty;
import static net.mindengine.galen.specs.Side.BOTTOM;
import static net.mindengine.galen.specs.Side.LEFT;
import static net.mindengine.galen.specs.Side.RIGHT;
import static net.mindengine.galen.specs.Side.TOP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.mindengine.galen.config.GalenConfig;
import net.mindengine.galen.parser.*;
import net.mindengine.galen.specs.Location;
import net.mindengine.galen.specs.Range;
import net.mindengine.galen.specs.Side;
import net.mindengine.galen.specs.reader.StringCharReader;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.MatcherAssert;
import org.junit.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ExpectationsTest {


    @BeforeClass
    public void init() throws IOException {
        deleteSystemProperty("galen.range.approximation");
        deleteSystemProperty("galen.reporting.listeners");
        GalenConfig.getConfig().reset();
    }

    @Test(dataProvider = "rangeTestData")
    public void expectRangeTest(String textForParsing, Range expected) {
        StringCharReader stringCharReader = new StringCharReader(textForParsing);
        Range range = new ExpectRange().read(stringCharReader);
        MatcherAssert.assertThat(range, is(expected));
    }
    
    
    @DataProvider
    public Object[][] rangeTestData() {
        return new Object[][]{
           {"10 to 15 px", Range.between(10.0, 15.0)},
           {"10.0 to 15.4 px", Range.between(10.0, 15.4)},
           {"10 to 15px", Range.between(10.0, 15.0)},
           {"10to15px", Range.between(10.0, 15.0)},
           {"-10to-15px", Range.between(-15.0, -10.0)},
           {"-10to-15.04px", Range.between(-15.04, -10.0)},
           {"10to15 px", Range.between(10.0, 15.0)},
           {"9 px", Range.exact(9.0)},
           {"9px", Range.exact(9.0)},
           {"9.01px", Range.exact(9.01)},
           {"   9px", Range.exact(9.0)},
           {"\t9px", Range.exact(9.0)},
           {"\t9\t\tpx", Range.exact(9.0)},
           {"-49px", Range.exact(-49.0)},
           {"~100px", Range.between(98.0, 102.0)},
           {"~1000px", Range.between(980.0, 1020.0)},
           {"~1px", Range.between(-1.0, 3.0)},
           {"~0px", Range.between(-2.0, 2.0)},
           {" ~0px", Range.between(-2.0, 2.0)},
           {">10px", Range.greaterThan(10.0)},
           {"> 10px", Range.greaterThan(10.0)},
           {"<10px", Range.lessThan(10.0)},
           {"< 10px", Range.lessThan(10.0)},
           {"15% of screen/width", Range.exact(15.0).withPercentOf("screen/width")},
           {"15.05% of screen/width", Range.exact(15.05).withPercentOf("screen/width")},
           {"15 to 40% of   screen/height", Range.between(15.0, 40.0).withPercentOf("screen/height")},
           {"15 to 40% of item-1/some-other-stuff/a/b/c2", Range.between(15.0, 40.0).withPercentOf("item-1/some-other-stuff/a/b/c2")},
           {"~40% of item-1/some-other-stuff/a/b/c2", Range.between(38.0, 42.0).withPercentOf("item-1/some-other-stuff/a/b/c2")},
           {"> 67 % of object/width", Range.greaterThan(67.0).withPercentOf("object/width")},
           {" < 30% of object/width", Range.lessThan(30.0).withPercentOf("object/width")},
           {" > 70% of parent/width", Range.greaterThan(70.0).withPercentOf("parent/width")}
        };
    }
    
    @Test(dataProvider = "provideBadRangeSamples")
    public void shouldGiveError_forIncorrectRanges(TestData<String> testData) {
        StringCharReader stringCharReader = new StringCharReader(testData.textForParsing);
        
        SyntaxException exception = null;
        try {
            new ExpectRange().read(stringCharReader);
        }
        catch (SyntaxException e) {
            exception = e;
        }
        
        assertThat("Exception should be", exception, is(notNullValue()));
        assertThat("Exception message should be", exception.getMessage(), is(testData.expected));
    }
    
    @DataProvider
    public Object[][] provideBadRangeSamples() {
        return new Object[][] {
            row("0", "Expecting \"px\", \"to\" or \"%\", got \"\""),
            row("0p", "Expecting \"px\", \"to\" or \"%\", got \"p\""),
            row("0 p", "Expecting \"px\", \"to\" or \"%\", got \"p\""),
            row("0PX", "Expecting \"px\", \"to\" or \"%\", got \"PX\""),
            row("10 to 20", "Missing ending: \"px\" or \"%\""),
            row("10 to 20p", "Missing ending: \"px\" or \"%\""),
            row("10 to 20%", "Missing value path for relative range"),
            row("10 to 20% of ", "Missing value path for relative range"),
            row("10% to 20% of ", "Missing value path for relative range"),
            //TODO add negative tests for ~ Range
        };
    }
    
    
    @Test(dataProvider = "wordTestData")
    public void expectWord(TestData<String> testData) {
        StringCharReader stringCharReader = new StringCharReader(testData.textForParsing);
        String word = new ExpectWord().read(stringCharReader);
        assertThat(word, is(testData.expected));
    }
    
    @DataProvider
    public Object[][] wordTestData() {
        return new Object[][]{
           row("object", "object"),
           row("  object", "object"),
           row("\tobject ", "object"),
           row("\t\tobject\tanother", "object"),
           row("o ject", "o"),
           row("o123-123124-_124/124|12qw!@#$%^^&*().<>?:\"[]{} ject", "o123-123124-_124/124|12qw!@#$%^^&*().<>?:\"[]{}"),
           row("   je ct", "je")
        };
    }
    
    @Test(dataProvider="wordWithBreakingSymbolTestData")
    public void expectWordWithBreakingSymbol(String text, char breakingSymbol, String expectedWord) {
        StringCharReader stringCharReader = new StringCharReader(text);
        String word = new ExpectWord().stopOnTheseSymbols(breakingSymbol).read(stringCharReader);
        assertThat(word, is(expectedWord));
    }
    
    @DataProvider
    public Object[][] wordWithBreakingSymbolTestData() {
        return new Object[][]{
            new Object[]{"Hi, John!", ',', "Hi"},
            new Object[]{" Hi, John!", ',', "Hi"},
            new Object[]{" HiJohn", 'o', "HiJ"},
            new Object[]{"HiJohn", '!', "HiJohn"}
        };
    }
    
    @Test(dataProvider = "sideTestData")
    public void expectSides(TestData<List<Side>> testData) {
        StringCharReader stringCharReader = new StringCharReader(testData.textForParsing);
        List<Side> sides = new ExpectSides().read(stringCharReader);
        
        Side[] expected = testData.expected.toArray(new Side[testData.expected.size()]);
        assertThat(sides.size(), is(expected.length));
        assertThat(sides, contains(expected));
    }
    
    @DataProvider
    public Object[][] sideTestData() {
        return new Object[][]{
           row("left right", sides(LEFT, RIGHT)),
           row("    \tleft\t  right  ", sides(LEFT, RIGHT)),
           row("   left   ", sides(LEFT)),
           row("top  left   ", sides(TOP, LEFT)),
           row("top  left  bottom ", sides(TOP, LEFT, BOTTOM))
        };
    }
    
    @Test(dataProvider = "locationsTestData")
    public void expectLocations(TestData<List<Location>> testData) {
        StringCharReader stringCharReader = new StringCharReader(testData.textForParsing);
        List<Location> sides = new ExpectLocations().read(stringCharReader);
        
        Location[] expected = testData.expected.toArray(new Location[testData.expected.size()]);
        assertThat(sides.size(), is(expected.length));
        assertThat(sides, contains(expected));
    }
    
    @DataProvider
    public Object[][] locationsTestData() {
        return new Object[][]{
           row("10 px left right, 10 to 20 px top bottom", locations(new Location(Range.exact(10), sides(LEFT, RIGHT)), new Location(Range.between(10, 20), sides(TOP, BOTTOM)))),
           row("10 px left, 10 to 20 px top bottom, 30px right", locations(new Location(Range.exact(10), sides(LEFT)), 
                   new Location(Range.between(10, 20), sides(TOP, BOTTOM)), 
                   new Location(Range.exact(30), sides(RIGHT)))),
           row("   10 px left right   ,   10 to 20 px top bottom  ", locations(new Location(Range.exact(10), sides(LEFT, RIGHT)), new Location(Range.between(10, 20), sides(TOP, BOTTOM)))),
           row("\t10 px left right\t,\t10 to 20 px\ttop\tbottom \t \t \t", locations(new Location(Range.exact(10), sides(LEFT, RIGHT)), new Location(Range.between(10, 20), sides(TOP, BOTTOM)))),
        };
    }
    
    @Test(dataProvider = "provideBadLocations")
    public void shouldGiveError_forIncorrectLocations(String text, String expectedErrorMessage) {
        StringCharReader stringCharReader = new StringCharReader(text);
        
        SyntaxException exception = null;
        try {
            new ExpectLocations().read(stringCharReader);
        }
        catch (SyntaxException e) {
            exception = e;
        }
        
        assertThat("Exception should be", exception, is(notNullValue()));
        assertThat("Exception message should be", exception.getMessage(), is(expectedErrorMessage));
    }
    
    @DataProvider
    public Object[][] provideBadLocations() {
        return new Object[][]{
            {"left", "Cannot parse number: \"\""},
            {"10px qwe", "Unknown side: \"qwe\""},
            {"10 to 30px qwe", "Unknown side: \"qwe\""},
            {"10 to 30% of screen/width qwe", "Unknown side: \"qwe\""},
            {"10px left qwe", "Unknown side: \"qwe\""},
            {"10px left, 20px qwe", "Unknown side: \"qwe\""},
            {"10px left, 20px left qwe", "Unknown side: \"qwe\""},
            {"10px left, right, top", "Cannot parse number: \"\""},
        };
    }


    @Test
    public void shouldParse_commaSeparatedKeyValue() {
        String text = ",param1 1, param2 v a l u e 2, booleanParam, param3 2.3, param1 2";
        List<Pair<String, String>> params = new ExpectCommaSeparatedKeyValue().read(new StringCharReader(text));

        assertThat(params.size(), is(5));
        assertThat(params.get(0).getKey(), is("param1"));
        assertThat(params.get(0).getValue(), is("1"));

        assertThat(params.get(1).getKey(), is("param2"));
        assertThat(params.get(1).getValue(), is("v a l u e 2"));

        assertThat(params.get(2).getKey(), is("booleanParam"));
        assertThat(params.get(2).getValue(), is(""));

        assertThat(params.get(3).getKey(), is("param3"));
        assertThat(params.get(3).getValue(), is("2.3"));

        assertThat(params.get(4).getKey(), is("param1"));
        assertThat(params.get(4).getValue(), is("2"));


    }

    @Test
    public void shouldParse_commaSeparatedKeyValue_2() {
        String text = "param1 1, param2 2";
        List<Pair<String, String>> params = new ExpectCommaSeparatedKeyValue().read(new StringCharReader(text));

        assertThat(params.size(), is(2));
        assertThat(params.get(0).getKey(), is("param1"));
        assertThat(params.get(0).getValue(), is("1"));

        assertThat(params.get(1).getKey(), is("param2"));
        assertThat(params.get(1).getValue(), is("2"));

    }
    
    
    private List<Location> locations(Location...locations) {
        return Arrays.asList(locations);
    }

    private List<Side> sides(Side...sides) {
        return Arrays.asList(sides);
    }

    private <T> Object[] row(String textForParsing, T expectedRange) {
        return new Object[]{new TestData<T>(textForParsing, expectedRange)};
    }


    private class TestData<T> {
        private String textForParsing;
        private T expected;
        
        public TestData(String textForParsing, T expected) {
            this.textForParsing = textForParsing;
            this.expected = expected;
        }
        
        @Override
        public String toString() {
            return StringEscapeUtils.escapeJava(textForParsing);
        }
    }
    
}
