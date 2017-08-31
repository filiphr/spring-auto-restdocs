/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package capital.scalable.restdocs.example.items;

import static capital.scalable.restdocs.example.items.ItemResponse.EnumType.ONE;
import static java.util.Collections.singletonList;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;

import capital.scalable.restdocs.example.items.ItemResponse.Attributes;
import capital.scalable.restdocs.example.items.ItemResponse.Metadata;
import lombok.Data;
import lombok.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Simple REST resource with CRUD operations.
 */
@RestController
@RequestMapping("/items")
public class ItemResource {

    private static final BigDecimal DECIMAL = new BigDecimal("1.11");
    private static final BigDecimal AMOUNT = new BigDecimal("3.14");

    private static final ItemResponse CHILD =
            new ItemResponse("child-1", "first child", null, null, null);

    private static final ItemResponse ITEM =
            new ItemResponse("1", "main item", new Metadata("meta1", 22),
                    new Attributes("first item", 1, true, DECIMAL, Money.of(AMOUNT, "EUR"), ONE),
                    singletonList(CHILD));

    /**
     * Returns item by ID.
     *
     * @param id ID of the item.
     * @return response
     */
    @RequestMapping("{id}")
    public ItemResponse getItem(@PathVariable("id") String id) {
        if ("1".equals(id)) {
            return ITEM;
        } else {
            throw new NotFoundException();
        }
    }

    /**
     * Lists all items.
     *
     * @return list of all items
     */
    @RequestMapping
    public ItemResponse[] allItems() {
        return new ItemResponse[]{ITEM, CHILD};
    }

    /**
     * Adds new item.
     *
     * @param itemUpdate Item information
     * @return response
     */
    @RequestMapping(method = POST)
    public ResponseEntity<Void> addItem(@RequestBody ItemUpdateRequest itemUpdate) {
        // New item with unique ID is stored and returned.
        URI location = ServletUriComponentsBuilder
                .fromUriString("/items")
                .path("/{id}")
                .buildAndExpand("2")
                .toUri();

        return ResponseEntity
                .created(location)
                .body(null);
    }

    /**
     * Updates existing item.
     *
     * @param id         Item ID.
     * @param itemUpdate Item information.
     * @return response
     */
    @RequestMapping(value = "{id}", method = PUT)
    public HttpEntity<ItemResponse> updateItem(@PathVariable("id") String id,
            @RequestBody ItemUpdateRequest itemUpdate) {
        return new HttpEntity<>(
                new ItemResponse(id, itemUpdate.getDescription(), null, null, null));
    }

    /**
     * Deletes item.
     * <br>
     * Item must exist.
     * <p>
     * Non existing items are ignored
     *
     * @param id Item ID
     */
    @RequestMapping(value = "{id}", method = DELETE)
    public void deleteItem(@PathVariable("id") String id) {
        // Item with the given ID is deleted.
    }

    /**
     * Retrieves a child of specified item.
     *
     * @param id      Item ID.
     * @param childId Child ID.
     * @return response
     */
    @RequestMapping("{id}/{child}")
    public ItemResponse getChild(@PathVariable String id,
            @PathVariable("child") String childId) {
        if ("1".equals(id) && "child-1".equals(childId)) {
            return CHILD;
        } else {
            throw new NotFoundException();
        }
    }

    /**
     * Searches for item based on lookup parameters.
     *
     * @param descMatch Lookup on description field.
     * @param hint      Lookup hint.
     * @return response
     */
    @RequestMapping("search")
    public Page<ItemResponse> searchItem(
            @RequestParam("desc") String descMatch,
            @RequestParam(required = false) Integer hint,
            Pageable page) {
        if (ITEM.getDescription().contains(descMatch)) {
            return new PageImpl<>(singletonList(ITEM), page, 1);
        } else {
            return new PageImpl<>(Collections.<ItemResponse>emptyList(), page, 0);
        }
    }

    /**
     * Executes a command on all items.
     */
    @RequestMapping(value = "process", method = POST)
    public String processAllItems(@RequestBody String command) {
        // process request as Command
        return "{ \"output\": \"processed\" }";
    }

    /**
     * Executes a command on an item.
     * <br>
     * This endpoint demos the basic support for @ModelAttribute.
     * <p>
     * Notes:
     * <ul>
     * <li>the request body is documented as it would be JSON,
     * but it is actually form-urlencoded</li>
     * <li>setting the type manually can help to get the right documentation
     * if the automatic document does not produce the right result.</li>
     * </ul>
     *
     * @param itemId Item ID.
     * @title Process One Item
     */
    @RequestMapping(value = "{itemId}/process", method = POST)
    public CommandResult processSingleItem(@PathVariable String itemId,
            @ModelAttribute Command command) {
        return new CommandResult(
                String.format("Command executed on item %s: %s", itemId, command.getCommand()));
    }

    @Data
    static class Command {
        /**
         * Command to execute
         */
        private String command;
    }

    @Value
    static class CommandResult {
        /**
         * Log output
         */
        private String output;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class NotFoundException extends RuntimeException {
    }
}