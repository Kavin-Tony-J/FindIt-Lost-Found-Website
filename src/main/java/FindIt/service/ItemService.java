
package FindIt.service;

import FindIt.model.Item;
import FindIt.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService
{
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository)
    {
        this.itemRepository = itemRepository;
    }

    public Item saveItem(Item item)
    {
        return itemRepository.save(item);
    }

    public List<Item> getAllItems()
    {
        return itemRepository.findAll();
    }

    public long getTotalCount()
    {
        return itemRepository.count();
    }

    public long getLostCount()
    {
        return itemRepository.countByStatus("LOST");
    }

    public long getFoundCount()
    {
        return itemRepository.countByStatus("FOUND");
    }

    public long getReturnedCount()
    {
        return itemRepository.countByStatus("RETURNED");
    }

    public boolean markReturned(Long id)
    {
        return itemRepository.findById(id)
            .map(item ->
            {
                if (!"RETURNED".equals(item.getStatus()))
                {
                    item.setStatus("RETURNED");
                    itemRepository.save(item);
                }

                return true;
            })
            .orElse(false);
    }
}
